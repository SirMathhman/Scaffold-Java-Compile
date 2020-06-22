package com.meti;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class Main {
	public static final Path COMPILE = Paths.get(".", "target", "compile");
	public static final Path CONFIG = Paths.get(".", "config", "java-compile.json");
	public static final Path SOURCE = Paths.get(".", "src", "main", "java");
	public static final Logger logger = Logger.getLogger("Scaffold");

	private Main() {
	}

	public static void main(String[] args) {
		run();
	}

	private static void prepareConfig() {
		ensureParent(CONFIG);
		if(!Files.exists(CONFIG)) {
			try {
				Files.createFile(CONFIG);
			} catch (IOException e) {
				logger.log(Level.WARNING, "Failed to create config file.");
			}
		}
	}

	private static void run() {
		prepareConfig();
		runCompilation();
		cleanup();
		moveClasses();
	}

	private static void runCompilation() {
		try {
			execute(buildCommand());
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to run compilation command.", e);
		}
	}

	private static void cleanup() {
		try {
			Files.walkFileTree(COMPILE, new DeleteFileVisitor());
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to cleanup class files.", e);
		}
	}

	private static void moveClasses() {
		try {
			moveClassesExceptionally();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to move classes.", e);
		}
	}

	private static void execute(Collection<String> command) throws IOException {
		Process builder = new ProcessBuilder(command.toArray(String[]::new))
				.directory(SOURCE.toFile())
				.start();
		String outputString = readStreamAsString(builder.getInputStream());
		String errorString = readStreamAsString(builder.getErrorStream());
		if (errorString.isBlank()) {
			if (outputString.isBlank()) {
				logger.log(Level.INFO, "Command produced no output.");
			} else {
				logger.log(Level.INFO, MessageFormat.format("Command produced output:{0}\t{1}",
						System.lineSeparator(), outputString));
			}
		} else {
			logger.log(Level.SEVERE, String.format("Failed to execute %s", command));
		}
	}

	private static List<String> buildCommand() throws IOException {
		List<String> command = new ArrayList<>();
		command.add("javac");
		command.addAll(collectJavaPaths());
		return command;
	}

	private static void moveClassesExceptionally() throws IOException {
		Files.walk(SOURCE)
				.filter(path -> path.toString().endsWith(".class"))
				.forEach(Main::move);
	}

	private static String readStreamAsString(InputStream inputStream) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		inputStream.transferTo(outputStream);
		return outputStream.toString();
	}

	private static List<String> collectJavaPaths() throws IOException {
		return Files.walk(SOURCE)
				.filter(path -> path.toString().endsWith(".java"))
				.map(SOURCE::relativize)
				.map(Path::toString)
				.collect(Collectors.toList());
	}

	private static void move(Path path) {
		Path in = SOURCE.relativize(path);
		Path out = COMPILE.resolve(in);
		ensureParent(out);
		move(path, out);
	}

	private static void ensureParent(Path path) {
		try {
			Path parent = path.getParent();
			if (!Files.exists(parent)) {
				Files.createDirectories(parent);
				logger.log(Level.FINER, String.format("Created parent directory %s", parent));
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, String.format("Failed to delete parent directory %s", path), e);
		}
	}

	private static void move(Path from, Path to) {
		try {
			Files.deleteIfExists(to);
			Files.move(from, to);
		} catch (IOException e) {
			logger.log(Level.WARNING, MessageFormat.format("Failed to move path from {0} to {1}", from, to), e);
		}
	}

	private static class DeleteFileVisitor extends SimpleFileVisitor<Path> {
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			Files.delete(file);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			Files.delete(dir);
			return FileVisitResult.CONTINUE;
		}
	}
}
