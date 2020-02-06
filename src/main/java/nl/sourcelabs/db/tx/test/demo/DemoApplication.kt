package nl.sourcelabs.db.tx.test.demo

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.util.*

@SpringBootApplication
@RestController
class DemoApplication {

    private val LOG = LoggerFactory.getLogger(DemoApplication::class.java)

    @Autowired
    private lateinit var someService: SomeService

    @GetMapping("/")
    fun get(): String {
        try {
            val nextLong = Random().nextInt(100).toLong()
            LOG.info("Starting for key: $nextLong")
            someService.doLongStuff(nextLong)
        } catch (e: Exception) {
            LOG.info("Coroutine failed: ${e.message}")
        }
        return "OK"
    }
}

@Service
@Transactional
class SomeService(private val sampleRepository: SampleRepository, private val lockManager: LockManager) {

    private val LOG = LoggerFactory.getLogger(SomeService::class.java)

    fun doLongStuff(key: Long) {
        val currentTransactionName = TransactionSynchronizationManager.getCurrentTransactionName()
        LOG.info("key: $key, currentTransactionName: $currentTransactionName")

        lockManager.tryWithLock(key = key, timeout = Duration.ofSeconds(1)) {
            LOG.info("inLock: $key")
            repeat(3) {
                LOG.info("Sleeping 1000ms for key: $key")
                Thread.sleep(1000)
                sampleRepository.find()
            }
        }
    }
}

/**
 * LockManager implementation that uses postgres transaction bound advisory locks.
 */
@Component
class LockManager(private val jdbcTemplate: JdbcTemplate) {
    private val log = LoggerFactory.getLogger(LockManager::class.java)

    /**
     * We need a transaction for the locking to actually work, but possible to participate in an ongoing transaction.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    fun <T> tryWithLock(key: Long, timeout: Duration, function: () -> T): T {
        lock(key, timeout)
        return function()
    }

    private fun lock(key: Long, timeout: Duration) {
        val timeoutMillis = timeout.toMillis()
        var count = 0

        log.info("Acquiring pg_try_advisory_xact_lock($key)")

        while (!jdbcTemplate.queryForObject("select pg_try_advisory_xact_lock(?)", Boolean::class.java, key)) {
            if (timeoutMillis - (1000 * count++) > 0) {
                log.info("Waiting for 1000 ms to acquire pg_try_advisory_xact_lock($key)")
                Thread.sleep(1000)
            } else {
                throw RuntimeException("Deadlock detected while pg_try_advisory_xact_lock with key: $key")
            }
        }
    }
}

@Repository
class SampleRepository(private val jdbcTemplate: JdbcTemplate) {
    fun find(): List<String> = jdbcTemplate.query("SELECT tablename FROM pg_catalog.pg_tables") { rs, _ -> rs.getString("tablename") }
}

fun main(args: Array<String>) {
    SpringApplication.run(DemoApplication::class.java, *args)
}