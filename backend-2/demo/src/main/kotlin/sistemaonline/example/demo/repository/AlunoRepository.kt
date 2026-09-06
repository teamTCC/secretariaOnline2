package sistemaonline.example.demo.repository

import org.springframework.data.jpa.repository.JpaRepository
import sistemaonline.example.demo.model.Aluno

interface AlunoRepository : JpaRepository<Aluno, Long>
