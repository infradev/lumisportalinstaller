package infradev.lumis.lumisportalinstaller.common.inject.interceptor;

import infradev.lumis.lumisportalinstaller.common.tools.Log;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

/**
 * Interceptor of Methods to be logged.
 * 
 * @author Alexandre Ribeiro de Souza
 */
public class LoggingMethodInterceptor implements MethodInterceptor {

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object returnValue;

		String className = Splitter.on("Impl").splitToList(invocation.getThis().getClass().getSimpleName()).get(0);
		String methodName = invocation.getMethod().getName();
		Object[] arguments = invocation.getArguments();

		List<String> methodNameSplitted = Splitter.on("_").splitToList(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, methodName));
		String[] methodPrefixArray = methodNameSplitted.get(0).split("(?!^)");
		int lastIndex = methodPrefixArray.length - 1;

		methodNameSplitted = methodNameSplitted.subList(1, methodNameSplitted.size());
		methodPrefixArray[0] = methodPrefixArray[0].toUpperCase();
		methodPrefixArray[lastIndex] = (methodPrefixArray[lastIndex].equals("e") ? "" : methodPrefixArray[lastIndex]) + "ing";
		String methodPrefix = Arrays.toString(methodPrefixArray).replaceAll("\\[|\\]|(, )", "");

		String methodDesc = Joiner.on(".").join(methodNameSplitted).replace("..", " ");

		if (null != methodName) {
			switch (methodName) {
			case "install":
				Log.info(String.format("  *** Configuring %s ***", className));
				break;
			case "unpackZipFile":
				Log.info(String.format("- Extracting %s", ((File) arguments[0]).getName()));
				break;
			case "downloadFile":
				Log.info(String.format("- Verifying %s metadata", arguments[0]));
				break;
			case "downloadToFile":
				Log.info(String.format("- Downloading %s", ((File) arguments[1]).getName()));
				break;
			case "executeSql":
				Log.info(String.format("  |- %s", arguments[0]));
				break;
			default:
				if (methodName.startsWith("execute")) {
					Log.info(String.format("- Executing %s: %s", methodDesc, arguments[0]));
				} else {
					Log.info(String.format("- %s %s", methodPrefix, methodDesc));
				}
				break;
			}
		}

		returnValue = invocation.proceed();

		if (null != methodName) {
			switch (methodName) {
			case "unpackZipFile":
				Log.info(String.format("  +- Finished extraction of %d files", returnValue));
				break;
			case "downloadToFile":
				Log.info(String.format("  +- Finished download of %s", ((File) arguments[1]).getName()));
				break;
			case "executeScript":
				Log.info(String.format("  +- Finished execution of %d statements", returnValue));
				break;
			}
		}

		return returnValue;
	}
}
