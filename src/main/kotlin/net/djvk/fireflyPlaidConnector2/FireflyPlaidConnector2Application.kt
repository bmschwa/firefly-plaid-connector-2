package net.djvk.fireflyPlaidConnector2

import net.djvk.fireflyPlaidConnector2.sync.Runner
import org.slf4j.LoggerFactory

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.event.ApplicationContextInitializedEvent
import org.springframework.boot.context.event.ApplicationFailedEvent
import org.springframework.boot.context.event.SpringApplicationEvent
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener

import java.lang.reflect.UndeclaredThrowableException


@Profile("!test")
@ConfigurationPropertiesScan(basePackages = ["net.djvk.fireflyPlaidConnector2.config.properties"])
@SpringBootApplication
class FireflyPlaidConnector2Application(
    private val runner: Runner,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun appReady() {
        /**
         * We're doing this here rather than in a bean init function or @PostConstruct to avoid
         *  things being launched automatically, thus making them easier to test
         */
        logger.debug("Beginning Run")
        runner.run()
    }

    @EventListener(ApplicationContextInitializedEvent::class)
    fun appContextInitialized(){
        logger.debug("App Context Initialized")
    }

    @EventListener(ApplicationFailedEvent::class)
    fun appFailedEvent(e: ApplicationFailedEvent){
        logger.error("Failure in application: %s".format(e))
    }

    @EventListener(SpringApplicationEvent::class)
    fun oH_boy(event: SpringApplicationEvent){

        System.out.println("post constructed")
    }



}

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger(FireflyPlaidConnector2Application::class.java)
    try {
        runApplication<FireflyPlaidConnector2Application>(*args)
    } catch(missing: ConfigDataResourceNotFoundException) {
        logger.error("Failed finding %s".format(missing.location))
        throw missing
    } catch(ute: UndeclaredThrowableException) {
        logger.error("Undeclared")
        throw ute
    }
    //catch (e: Exception){
    //   throw e
    //}

}
