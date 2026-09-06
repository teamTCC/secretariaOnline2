package br.ufpr.sept.so2.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

@AnalyzeClasses(
    packages = ["br.ufpr.sept.so2"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class DomainLayerArchTest {
    @ArchTest
    val domainMustNotDependOnSpring: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..",
            )
            .because("domain deve permanecer puro (sem framework de persistência ou Spring)")

    @ArchTest
    val controllersMustNotDependOnJpa: ArchRule =
        noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure.persistence..")
            .because("controllers não persistem: JPA fica em adapters; application chama ports ou o repositório do próprio módulo")

    @ArchTest
    val bffMustNotDependOnJpa: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..modules.bff..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure.persistence..")
            .because("BFF agrega via ports de leitura; não injeta JPA de outros bounded contexts")
}
