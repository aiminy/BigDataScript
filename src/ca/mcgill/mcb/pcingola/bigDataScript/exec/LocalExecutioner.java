package ca.mcgill.mcb.pcingola.bigDataScript.exec;

import java.util.ArrayList;
import java.util.HashMap;

import ca.mcgill.mcb.pcingola.bigDataScript.cluster.host.Host;
import ca.mcgill.mcb.pcingola.bigDataScript.osCmd.CmdRunner;
import ca.mcgill.mcb.pcingola.bigDataScript.util.Timer;

/**
 * Execute tasks in local computer. 
 * Executed the command immediately without controlling host resources.
 *  
 * @author pcingola
 */
public class LocalExecutioner extends Executioner {

	public static String LOCAL_EXEC_COMMAND[] = { "bds", "exec" };
	private static final String[] ARGS_ARRAY_TYPE = new String[0];
	HashMap<String, CmdRunner> cmdById;

	public LocalExecutioner() {
		super();
		cmdById = new HashMap<String, CmdRunner>();
	}

	@Override
	public void add(Task task) {
		super.add(task);
		run(task, null); // Run task right away
	}

	@Override
	public synchronized boolean finished(String id) {
		cmdById.remove(id);
		return super.finished(id);
	}

	@Override
	protected boolean killTask(Task task) {
		CmdRunner cmd = cmdById.get(task.getId());
		if (cmd == null) return false;
		cmd.kill();
		return true;
	}

	@Override
	protected boolean runLoop() {
		// Nothing to do
		return false;
	}

	/**
	 * Execute on local computer. 
	 * Host parameter is ignored
	 */
	@Override
	protected boolean runTask(Task task, Host host) {
		task.createProgramFile(); // We must create a program file

		ArrayList<String> args = new ArrayList<String>();
		for (String arg : LOCAL_EXEC_COMMAND)
			args.add(arg);

		// Add 'bds exec' parameters
		args.add(task.getResources().getTimeout() + "");
		args.add(task.getStdoutFile());
		args.add(task.getStderrFile());
		args.add(task.getExitCodeFile());
		args.add(task.getProgramFileName());

		String cmdStr = "";
		for (String arg : args)
			cmdStr += arg + " ";

		// Run command
		if (debug) Timer.showStdErr("Running command: " + cmdStr);
		CmdRunner cmd = new CmdRunner(task.getId(), args.toArray(ARGS_ARRAY_TYPE));
		cmd.setCmdStats(task);
		cmd.setExecutioner(this);
		cmdById.put(task.getId(), cmd);
		cmd.start();

		return true;
	}

}