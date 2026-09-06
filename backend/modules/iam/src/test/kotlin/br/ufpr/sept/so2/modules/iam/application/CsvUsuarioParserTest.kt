package br.ufpr.sept.so2.modules.iam.application

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class CsvUsuarioParserTest :
    StringSpec({
        "parses valid rows and defaults role to ALUNO" {
            val csv =
                """
                nome,email,grr
                Maria Silva,maria@ufpr.br,GRR20220001
                """.trimIndent()
            val result = CsvUsuarioParser.parse(csv)
            result.errors shouldHaveSize 0
            result.rows shouldHaveSize 1
            result.rows[0].email shouldBe "maria@ufpr.br"
            result.rows[0].roleCode shouldBe "ALUNO"
            result.rows[0].grr shouldBe "GRR20220001"
        }

        "rejects missing header columns" {
            val result = CsvUsuarioParser.parse("foo,bar\n1,2")
            result.rows shouldHaveSize 0
            result.errors shouldHaveSize 1
        }

        "collects invalid email without dropping valid rows" {
            val csv =
                """
                nome,email,role
                Ok User,ok@ufpr.br,ALUNO
                Bad User,sem-arroba,ALUNO
                """.trimIndent()
            val result = CsvUsuarioParser.parse(csv)
            result.rows shouldHaveSize 1
            result.errors shouldHaveSize 1
            result.errors.first()["linha"] shouldBe 3
        }

        "returns error for empty file" {
            val result = CsvUsuarioParser.parse("   \n")
            result.rows shouldHaveSize 0
            result.errors shouldHaveSize 1
        }
    })
