package nl.sourcelabs.db.tx.test.demo

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@SpringBootApplication
@EnableTransactionManagement
class DemoApplication

@Repository
class SampleRepository(private val jdbcTemplate: JdbcTemplate) {

    fun find(): List<String> = jdbcTemplate.query("select * from payment_plan", RowMapper<String> { rs, b ->
        rs.getString(0)
    })
}

@Service
class SomeService(private val sampleRepository: SampleRepository, private val platformTransactionManager: PlatformTransactionManager) {

    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun doLongStuff() {
        sampleRepository.find()

        runBlocking {
            repeat(3) {
                println("Sleeping for 1000ms")
                delay(1000)
            }
        }
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(DemoApplication::class.java, *args)
}
