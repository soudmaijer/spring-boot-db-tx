package nl.sourcelabs.db.tx.test.demo

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class DemoApplicationTests {

    @Autowired
    private lateinit var someService: SomeService

    @Test
    fun runAsyncThreads() {
        runBlocking {
            repeat(10) {
                async {
                    someService.doLongStuff()
                }
            }
        }
    }
}