import Encoding.*
import PrinterFormat.*
import kotlinx.cli.*
import parsers.*
import parsers.OutputFormat.DIMACS
import printers.*
import java.io.BufferedReader
import java.io.File
import java.io.PrintWriter

fun readInts() = readLine()!!.split(" ").run { IntArray(size) { get(it).toInt() } }
fun readChar() = readLine()!![0]

enum class Encoding {
    ARRAY,
    REDUCED_ARRAY,
    LATIN_ONEHOT,
    LATIN_LOG,
    REDUCED_LATIN_ONEHOT,
    REDUCED_LATIN_LOG
}

enum class PrinterFormat {
    CNF
}


@OptIn(ExperimentalCli::class)
fun main(args: Array<String>) {
    val argParser = ArgParser("latin-square-encoding-generator")

    class DecodeConfig : Subcommand("decode", "Decode result of solver") {
        val encoding by option(
            ArgType.Choice<Encoding>(),
            "encoding",
            shortName = "e",
            description = "encoding of solved file"
        ).required()
        val inputFile by option(
            ArgType.String,
            "inputFile",
            shortName = "i",
            description = "filename of dimacs file"
        ).required()
        val fileFormat by option(
            ArgType.Choice<OutputFormat>(),
            "format",
            shortName = "f",
            description = "format of dimacs file"
        ).default(DIMACS)
        val n by option(ArgType.Int, "matrixSize", shortName = "n", description = "size of latinSquare").default(10)
        val q by option(ArgType.Int, "matrixCount", shortName = "q", description = "count of matrixes").default(3)
        override fun execute() {
            val parserInit: (Int, Int, BufferedReader, OutputFormat) -> Parser = when (encoding) {
                ARRAY -> ::ArrayParser
                REDUCED_ARRAY -> ::ReducedArrayParser
                LATIN_ONEHOT -> ::LatinParser
                LATIN_LOG -> ::LogLatinParser
                REDUCED_LATIN_ONEHOT -> ::ReducedLatinParser
                REDUCED_LATIN_LOG -> ::ReducedLogLatinParser
            }
            val parser = parserInit(n, q, File(inputFile).bufferedReader(), fileFormat)
            parser.parse()
        }
    }
    class EncodeConfig : Subcommand("encode", "Encode latin object to solver") {
        val encoding by option(
            ArgType.Choice<Encoding>(),
            "encoding",
            shortName = "e",
            description = "encoding of object to solve"
        ).required()
        val outputFile by option(
            ArgType.String,
            "outputFile",
            shortName = "o",
            description = "filename of dimacs file"
        )
        val cnfPrinterFormat by option(
            ArgType.Choice<PrinterFormat>(),
            "cnfPrintFormat",
            shortName = "pf",
            description = "print format of cnf"
        ).default(CNF)
        val n by option(ArgType.Int, "matrixSize", shortName = "n", description = "size of latinSquare").default(10)
        val q by option(ArgType.Int, "matrixCount", shortName = "q", description = "count of matrixes").default(3)
        val r by option(ArgType.Int, "index", shortName = "i", description = "index of quaziOrthogonality").required()

        override fun execute() {
            val cnfEncoderBuilder: LatinCnfEncoderBuilder = when (encoding) {
                ARRAY -> OrthogonalArrayEncoderBuilder(false)
                LATIN_LOG -> LogLatinSquareEncoderBuilder(false)
                LATIN_ONEHOT -> LatinSquareEncoderBuilder(false)
                REDUCED_ARRAY -> OrthogonalArrayEncoderBuilder(true)
                REDUCED_LATIN_LOG -> LogLatinSquareEncoderBuilder(true)
                REDUCED_LATIN_ONEHOT -> LatinSquareEncoderBuilder(true)
            }
            val cnf = cnfEncoderBuilder(n, q, r).cnf()
            val printWriter = outputFile?.let { filename ->
                File(filename).printWriter()
            } ?: PrintWriter(System.out, true)
            val cnfPrinter = when (cnfPrinterFormat) {
                CNF -> CnfPrinter(printWriter)
            }
            cnfPrinter.use { it.print(cnf) }
        }
    }

    argParser.subcommands(EncodeConfig(), DecodeConfig())
    argParser.parse(args)

//    val char = 'w'
//    if (char == 'r') {
//        val printer = LatinSquarePrinter(10, 2, 2, File("inputs/incremental/mainInput").printWriter())
//        printer.print()
//        val (n) = readInts()
//        LatinParser(5, 3, File("outputHorni").bufferedReader(), OutputFormat.MAXSAT).parse()
//        val parser = LatinParser(7, 3, File("outputIncd85").bufferedReader(), OutputFormat.PAINLESS)
//        val n = 5
//        val k = 3
//        val cnf = LatinSquareBuilder(n, k, 25).cnf()
//        val horny = HornifyCnfBuilder(cnf, n * n * n * k, false).cnf()
//        CnfPrinter(File("horni").printWriter()).print(horny)
//        parser.parse()
//        val arrayTmp = listOf((5736 until 5736 + 48).toList().reversed(), (10490 until 10490 + 48).toList().reversed())
//        val array = arrayTmp[0].indices.flatMap { i ->
//            arrayTmp.map { it[i] }
//        }
////        val array = arrayTmp.flatten()
//        val current = mutableListOf<Expression>()
//        for (i in array) {
//            current += not(Variable("$i"))
//            File("inputs/incremental/mainInputdExpr${current.size - 1}").printWriter().use {
//                it.print(and(current))
//            }
//        }
//        val cnf = ReadyLatinSquarePrinter(parser, 7, intArrayOf(100, 100), 3,1).cnf()
//        CnfPrinter(File("inputs/incremental/mainInputd").printWriter()).print(cnf)
//        parser.parse()
//    } else {
//        val cnf = LatinSquareBuilder(9, 2, 81).cnf()
//        varCounter = 0
//
//        CNFWithMetaPrinter(File("inputonehot").printWriter(), File("meta").printWriter()).print(
//            CNFWithMetaInfo(cnf, (1..3).flatMap { initMatrix(7, 25, 1).flatten() })
//        )
//        val (n, q) = readInts()
//        varCounter = 0
//        val parser = LatinParser(8, 2, File("output").bufferedReader(), OutputFormat.MINISAT).parse()
//        ReadyLatinSquarePrinter(parser, intArrayOf(65,65,65), 3,79, File("input").printWriter()).print()
//        for (k in 73..73 step 2) {
//        val array = (21705 until 21803).reversed().toList()
//        val array = arrayTmp[0].indices.flatMap { i ->
//            arrayTmp.map { it[i] }
//        }
//        println(array)
//        val current = mutableListOf<Expression>()
//        for (i in array) {
//            current += not(Variable("$i"))
//            File("inputs/incremental/mainInputExpr${current.size - 1}").printWriter().use {
//                it.println(and(current))
//            }
//        }
//        ReducedLatinSquarePrinter(10, 3, 0, File("inputReduced").printWriter()).print()
//            println("$k, reducedLatin")
//        }
//        for (k in 71..77 step 2) {
//        ReducedLatinSquarePrinter(10, 3, 1, File("inputs/incremental/mainInput").printWriter()).print()
//            println("$k, Latin")
//        }
//        for (k in 71..77 step 2) {
//            OrthogonalArrayPrinter(10, 5, k, File("inputs/inputArray$k").printWriter()).print()
//            println("$k, array")
//        }
//        for (k in 71..77 step 2) {
//            ReducedOrthogonalArrayPrinter(10, 5, k, File("inputs/inputReducedArray$k").printWriter()).print()
//            println("$k, reducedArray")
//        }
//    }
}