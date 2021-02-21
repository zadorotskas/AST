import java.io.File


fun main(args: Array<String>) {
    val input = File(args[0])

    val listOfFiles = Utils.getKtFiles(input)

    val ast = AST(listOfFiles)

    ast.parseFiles()
    ast.countMetrics()

    Utils.printMetrics(ast)

    Utils.writeMetricsInJsonFile(args[1], ast)
}
