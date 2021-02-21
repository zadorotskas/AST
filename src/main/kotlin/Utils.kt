import com.google.gson.Gson
import java.io.File

class Utils {
    companion object {
        fun getKtFiles(file: File): List<File> {
            val listOfFiles = mutableListOf<File>()
            file.walkTopDown().forEach {
                if (it.extension == "kt") {
                    listOfFiles.add(it)
                }
            }
            return listOfFiles
        }


        fun printMetrics(ast: AST) {
            println("Average depth of inheritance: ${ast.avgDepth}")
            println("Maximum depth of inheritance: ${ast.maxDepth}")
            println("A: ${ast.aMetric}")
            println("B: ${ast.bMetric}")
            println("C: ${ast.cMetric}")
            println("Average numbers of fields in classes: ${ast.avgNumberOfFields}")
            println("Average numbers of overridden methods: ${ast.avgNumbersOfOverriddenMethods}")
        }

        fun writeMetricsInJsonFile(fileName: String, ast: AST) {
            val metrics = Metrics(
                ast.avgDepth,
                ast.maxDepth,
                ast.avgNumberOfFields,
                ast.avgNumbersOfOverriddenMethods,
                ast.aMetric, ast.bMetric, ast.cMetric
            )
            val json = Gson().toJson(metrics)
            val jsonFile = File(fileName)
            if (!jsonFile.exists()) jsonFile.createNewFile()
            jsonFile.writeText(json)
        }
    }
}