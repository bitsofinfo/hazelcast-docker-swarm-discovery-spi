package org.bitsofinfo.hazelcast.discovery.docker.swarm.test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.LogEvent;

public class SystemPrintLogger implements ILogger {
	
	private DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	@Override
	public void finest(String message) {
		System.out.println("FINEST " + message);
	}

	@Override
	public void finest(Throwable thrown) {
		System.out.println("FINEST " + thrown.getMessage());
	}

	@Override
	public void finest(String message, Throwable thrown) {
		System.out.println("FINEST " + message + " " + thrown.getMessage());
	}

	@Override
	public boolean isFinestEnabled() {
		return true;
	}

	@Override
	public void fine(String message) {
		System.out.println("FINE " + message);
	}

	@Override
	public boolean isFineEnabled() {
		return true;
	}

	@Override
	public void info(String message) {
		System.out.println("INFO " + message);
	}

	@Override
	public void warning(String message) {
		System.out.println("WARNING " + message);
	}

	@Override
	public void warning(Throwable thrown) {
		System.out.println("WARNING " + thrown.getMessage());
	}

	@Override
	public void warning(String message, Throwable thrown) {
		System.out.println("WARNING " + message + " " + thrown.getMessage());
	}

	@Override
	public void severe(String message) {
		System.out.println("SEVERE " + message);
	}

	@Override
	public void severe(Throwable thrown) {
		System.out.println("SEVERE " + thrown.getMessage());
	}

	@Override
	public void severe(String message, Throwable thrown) {
		System.out.println("SEVERE " + message + " " + thrown.getMessage());
	}

	@Override
	public void log(Level level, String message) {
		System.out.println(level.getName()+" " + message);
	}

	@Override
	public void log(Level level, String message, Throwable thrown) {
		System.out.println(level.getName()+" " + message + " " + thrown.getMessage());
	}

	@Override
	public void log(LogEvent logEvent) {
		LogRecord r =  logEvent.getLogRecord();
		System.out.println("" + r.getLevel().getName() + " " + r.getMessage());
	}

	@Override
	public Level getLevel() {
		return Level.FINEST;
	}

	@Override
	public boolean isLoggable(Level level) {
		return true;
	}

}
