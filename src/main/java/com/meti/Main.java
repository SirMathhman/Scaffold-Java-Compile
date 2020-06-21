package com.meti;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
	public static final Path COMPILE = Paths.get(".", "target", "compile");
	public static final Path SOURCE = Paths.get(".", "src", "main", "java");

	public static void main(String[] args) {
		new Main().run();
	}

	private void run() {
		try {
			List<String> command = buildCommand();
			execute(command);

			Files.walk(SOURCE)
					.filter(path -> path.toString().endsWith(".class"))
					.forEach(this::move);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void execute(List<String> command) throws IOException {
		Process builder = new ProcessBuilder(command.toArray(String[]::new))
				.directory(SOURCE.toFile())
				.start();
		builder.getInputStream().transferTo(System.out);
		builder.getErrorStream().transferTo(System.err);
	}

	private List<String> buildCommand() throws IOException {
		List<String> command = new ArrayList<>();
		command.add("javac");
		Files.walk(SOURCE)
				.filter(path -> path.toString().endsWith(".java"))
				.map(SOURCE::relativize)
				.map(Path::toString)
				.forEach(command::add);
		return command;
	}

	private void move(Path path) {
		Path in = SOURCE.relativize(path);
		Path out = COMPILE.resolve(in);
		try {
			Path parent = out.getParent();
			if (!Files.exists(parent)) Files.createDirectories(parent);
			Files.deleteIfExists(out);
			Files.move(path, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
