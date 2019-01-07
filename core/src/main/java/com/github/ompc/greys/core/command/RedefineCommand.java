package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.advisor.Enhancer;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.affect.EnhancerAffect;
import com.github.ompc.greys.core.util.affect.RowAffect;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

@Cmd(name = "redefine", sort = 11, summary = "redefine classes",
        eg = {
                "redefine /tmp/Test.class",
        })
public class RedefineCommand implements Command {

	private static final int MAX_FILE_SIZE = 10 * 1024 * 1024;

	@IndexArg(index = 0, name = "class-path",isRequired = true, summary = "class path")
	private String path;

    @Override
    public Action getAction() {

		return new RowAction() {

			@Override
			public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

				final EnhancerAffect enhancerAffect = Enhancer.reset(inst);

				if(path == null | path.length() == 0){
					printer.print("path is error").finish();
					return new RowAffect(enhancerAffect.cCnt());
				}
				File file = new File(path);

				if (!file.exists()) {
					printer.print("file not exist").finish();
					return new RowAffect(enhancerAffect.cCnt());
				}
				;

				if (!file.isFile()) {
					printer.print("not a file").finish();
					return new RowAffect(enhancerAffect.cCnt());
				}
				;

				if (file.length() >= MAX_FILE_SIZE) {
					printer.print("out of range").finish();
					return new RowAffect(enhancerAffect.cCnt());
				}
				;

				RandomAccessFile f = null;
				try {
					f = new RandomAccessFile(path, "r");
					final byte[] bytes = new byte[(int) f.length()];
					f.readFully(bytes);

					final String clazzName = readClassName(bytes);

					List<ClassDefinition> definitions = new ArrayList<ClassDefinition>();
					for (Class<?> clazz : inst.getAllLoadedClasses()) {
						if (clazzName.equals(clazz.getName())) {
							definitions.add(new ClassDefinition(clazz, bytes));
						}
					}

					inst.redefineClasses(definitions.toArray(new ClassDefinition[0]));
					printer.println("redefine success, size: " + definitions.size() + "\n").finish();
				} catch (Exception e) {
					printer.println(e.getMessage()).finish();
				} finally {
					if (f != null) {
						try {
							f.close();
						} catch (IOException e) {
							// ignore
						}
					}
				}
				return new RowAffect(enhancerAffect.cCnt());
			}

			;

		};
	}

	private static String readClassName(final byte[] bytes) {
		return new ClassReader(bytes).getClassName().replace("/", ".");
	}

}
