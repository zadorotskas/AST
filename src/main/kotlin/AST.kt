import kastree.ast.Node
import kastree.ast.Visitor
import kastree.ast.psi.Parser
import java.io.File

class AST(private val listOfFiles: List<File>) {
    private val numbersOfFields = mutableMapOf<String, Int>()
    private val numbersOfOverriddenMethods = mutableMapOf<String, Int>()
    private val inheritanceTree = InheritanceTree()


    var avgDepth = 0.0
    var maxDepth = 0

    var aMetric = 0
    var bMetric = 0
    var cMetric = 0

    var avgNumberOfFields = 0.0
    var avgNumbersOfOverriddenMethods = 0.0


    private val fullNamesOfClassesAndInterfaces = mutableListOf<List<String>>()
    private val allFiles = mutableListOf<Node.File>()


    fun parseFiles() {
        listOfFiles.forEach { ktFile ->
            val code = ktFile.readText().trimIndent()
            try {
                val file = Parser.parseFile(code)
                allFiles.add(file)
                val path: MutableList<String> = file.pkg?.names as? MutableList<String> ?: mutableListOf()

                file.decls.forEach {
                    addToFullNames(it, path)
                }
            } catch (e: Parser.ParseError) {
                // Kastree library hasn't been updated for 1.5 years so now it cannot parse all kt files :(
                println("${ktFile.name} is unread")
            } catch (e: java.lang.IllegalStateException) {
                println("${ktFile.name} is unread")
            }
        }
    }

    fun countMetrics() {
        allFiles.forEach { file ->
            val listOfImports = file.imports.map { import -> import.names }
            val packageList: List<String> = file.pkg?.names ?: emptyList()

            val packageName: String = packageList.joinToString(separator = ".", postfix = ".")

            file.decls.forEach {
                addToInheritanceTree(
                    it,
                    packageName,
                    listOfImports,
                    packageList
                )
            }

            visitNodes(file, packageName)
        }

        if (numbersOfFields.isNotEmpty()) {
            avgNumberOfFields = numbersOfFields.values.toList().fold(0) { res, number -> res + number }
                .toDouble() / numbersOfFields.size
        }

        if (numbersOfOverriddenMethods.isNotEmpty()) {
            avgNumbersOfOverriddenMethods =
                numbersOfOverriddenMethods.values.toList().fold(0) { res, number -> res + number }
                    .toDouble() / numbersOfOverriddenMethods.size
        }

        avgDepth = inheritanceTree.getAvgDepth()
        maxDepth = inheritanceTree.getMaxDepth()

        println("Number of all files: ${listOfFiles.size}")
        println("Number of read files: ${allFiles.size}")
    }

    private fun addToInheritanceTree(
        v: Node?,
        currentPath: String,
        listOfImports: List<List<String>>,
        currentPackage: List<String>
    ) {
        if (v is Node.Decl.Structured &&
            (v.form == Node.Decl.Structured.Form.CLASS || v.form == Node.Decl.Structured.Form.INTERFACE)
        ) {
            val isInterface = v.form == Node.Decl.Structured.Form.INTERFACE
            val parents = mutableMapOf<String, Boolean>()
            v.parents.forEach {
                when (it) {
                    is Node.Decl.Structured.Parent.CallConstructor -> {
                        val nameInList = it.type.pieces.map { piece -> piece.name }
                        val name = getFullName(listOfImports, nameInList, currentPackage)
                        parents[name] = false
                    }
                    is Node.Decl.Structured.Parent.Type -> {
                        val nameInList = it.type.pieces.map { piece -> piece.name }
                        val name = getFullName(listOfImports, nameInList, currentPackage)
                        parents[name] = true
                    }
                }
            }
            inheritanceTree.addNode(currentPath + v.name, parents, isInterface)

            v.members.forEach {
                addToInheritanceTree(
                    it,
                    "$currentPath${v.name}.",
                    listOfImports,
                    currentPackage
                )
            }
        }
    }

    private fun addToFullNames(
        v: Node?,
        currentPath: MutableList<String>
    ) {
        if (v is Node.Decl.Structured &&
            (v.form == Node.Decl.Structured.Form.CLASS || v.form == Node.Decl.Structured.Form.INTERFACE)
        ) {
            currentPath.add(v.name)
            fullNamesOfClassesAndInterfaces.add(currentPath.toList())
            v.members.forEach {
                addToFullNames(it, currentPath)
            }
            currentPath.removeAt(currentPath.size - 1)
        }
    }

    private fun getFullName(
        imports: List<List<String>>,
        name: List<String>,
        currentPackage: List<String>
    ): String {
        val res = mutableListOf<String>()
        imports.forEach {
            res.addAll(it)
            if (it[it.size - 1] == name[0]) {
                res.removeAt(res.size - 1)
            }
            res.addAll(name)
            if (fullNamesOfClassesAndInterfaces.contains(res)) return res.joinToString(separator = ".")
            res.clear()
        }
        res.addAll(currentPackage)
        res.addAll(name)
        if (fullNamesOfClassesAndInterfaces.contains(res)) return res.joinToString(separator = ".")
        return name.joinToString(separator = ".")
    }

    private val listOfAssignmentOperations = listOf(
        Node.Expr.BinaryOp.Token.ASSN,
        Node.Expr.BinaryOp.Token.ADD_ASSN,
        Node.Expr.BinaryOp.Token.DIV_ASSN,
        Node.Expr.BinaryOp.Token.MOD_ASSN,
        Node.Expr.BinaryOp.Token.MUL_ASSN,
        Node.Expr.BinaryOp.Token.SUB_ASSN
    )

    private val listOfConditionalOperations = listOf(
        Node.Expr.BinaryOp.Token.GT,
        Node.Expr.BinaryOp.Token.GTE,
        Node.Expr.BinaryOp.Token.LT,
        Node.Expr.BinaryOp.Token.LTE,
        Node.Expr.BinaryOp.Token.EQ,
        Node.Expr.BinaryOp.Token.NEQ
    )

    private fun visitNodes(file: Node.File, packageName: String) {
        Visitor.visit(file) { v, parent ->
            if (parent is Node.Decl.Structured && v is Node.Decl.Property) {
                val name: String = packageName + parent.name
                numbersOfFields[name] = numbersOfFields.getOrDefault(name, 0) + 1
            }

            if (v is Node.Decl.Func && v.mods.contains(Node.Modifier.Lit(Node.Modifier.Keyword.OVERRIDE))) {
                val name = v.name.toString()
                numbersOfOverriddenMethods[name] = numbersOfOverriddenMethods.getOrDefault(name, 0) + 1
            }

            if (v is Node.Expr.UnaryOp) {
                val token = v.oper.token
                if (token == Node.Expr.UnaryOp.Token.DEC || token == Node.Expr.UnaryOp.Token.INC) {
                    aMetric++
                }
            }

            if (v is Node.Expr.BinaryOp) {
                val operator = v.oper
                if (operator is Node.Expr.BinaryOp.Oper.Token) {
                    val token = operator.token

                    if (token in listOfAssignmentOperations) {
                        aMetric++
                    }

                    if (token in listOfConditionalOperations) {
                        cMetric++
                    }
                }
            }

            if (v is Node.Expr.If && v.elseBody != null) {
                cMetric++
            }

            if (v is Node.Expr.Try) {
                cMetric += 1 + v.catches.size
            }

            if (v is Node.Expr.When) {
                cMetric += v.entries.size
            }

            if (v is Node.Expr.Call) {
                bMetric++
            }
        }
    }
}