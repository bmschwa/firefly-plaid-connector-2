package net.djvk.fireflyPlaidConnector2

import net.djvk.fireflyPlaidConnector2.sync.Runner

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
    @EventListener(ApplicationReadyEvent::class)
    fun appReady() {
        /**
         * We're doing this here rather than in a bean init function or @PostConstruct to avoid
         *  things being launched automatically, thus making them easier to test
         */
        runner.run()
    }

    @EventListener(ApplicationContextInitializedEvent::class)
    fun postConstructed(){
        System.out.println("post constructed")
    }

    @EventListener(ApplicationFailedEvent::class)
    fun postConstructed2(){
        System.out.println("post constructed")
        throw Exception()
    }

    @EventListener(SpringApplicationEvent::class)
    fun oH_boy(event: SpringApplicationEvent){

        System.out.println("post constructed")
    }



}

fun main(args: Array<String>) {
    try {
        runApplication<FireflyPlaidConnector2Application>(*args)
    } catch(missing: ConfigDataResourceNotFoundException) {
        System.out.println("Failed finding %s".format(missing.location))
        throw missing
    } catch(ute: UndeclaredThrowableException) {
        System.out.println("DOH!")
    }
    catch (e: Exception){
        System.out.println("DOH!")
    }

}
