package sistemaonline.example.demo.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sistemaonline.example.demo.model.Disciplina

@Repository
interface DisciplinaRepository : JpaRepository<Disciplina, Long>
