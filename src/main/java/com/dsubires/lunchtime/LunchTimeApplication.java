package com.dsubires.lunchtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase prinicpal del proyecto. La función de este proyecto es obtener
 * diariamente (de lunes a viernes) información sobre el menú del restaurante
 * edificio cajamar y publicarla en twitter y telegram (08:45 am).
 * 
 * @author dsubires
 *
 */
@SpringBootApplication
@EnableScheduling
public class LunchTimeApplication {

	public static void main(String[] args) {
		SpringApplication.run(LunchTimeApplication.class, args);
	}

}
