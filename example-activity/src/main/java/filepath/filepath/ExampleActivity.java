package filepath.filepath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.aspectj.util.FileUtil;

import net.sf.taverna.t2.invocation.InvocationContext;
import net.sf.taverna.t2.reference.ExternalReferenceSPI;
import net.sf.taverna.t2.reference.Identified;
import net.sf.taverna.t2.reference.ReferenceService;
import net.sf.taverna.t2.reference.ReferenceSet;
import net.sf.taverna.t2.reference.T2Reference;
import net.sf.taverna.t2.reference.impl.external.file.FileReference;
import net.sf.taverna.t2.reference.impl.external.http.HttpReference;
import net.sf.taverna.t2.workflowmodel.processor.activity.AbstractAsynchronousActivity;
import net.sf.taverna.t2.workflowmodel.processor.activity.ActivityConfigurationException;
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivity;
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivityCallback;

public class ExampleActivity extends
		AbstractAsynchronousActivity<ExampleActivityConfigurationBean>
		implements AsynchronousActivity<ExampleActivityConfigurationBean> {

	/*
	 * Best practice: Keep port names as constants to avoid misspelling. This
	 * would not apply if port names are looked up dynamically from the service
	 * operation, like done for WSDL services.
	 */
	private static final String IN_DATA = "data";
	private static final String OUT_URL = "url";
	private static final String OUT_REPORT = "report";
	
	private ExampleActivityConfigurationBean configBean;

	@Override
	public void configure(ExampleActivityConfigurationBean configBean)
			throws ActivityConfigurationException {

		// Any pre-config sanity checks
		if (configBean.getExampleString().equals("invalidExample")) {
			throw new ActivityConfigurationException(
					"Example string can't be 'invalidExample'");
		}
		// Store for getConfiguration(), but you could also make
		// getConfiguration() return a new bean from other sources
		this.configBean = configBean;

		// OPTIONAL: 
		// Do any server-side lookups and configuration, like resolving WSDLs

		// myClient = new MyClient(configBean.getExampleUri());
		// this.service = myClient.getService(configBean.getExampleString());

		
		// REQUIRED: (Re)create input/output ports depending on configuration
		configurePorts();
	}

	protected void configurePorts() {
		// In case we are being reconfigured - remove existing ports first
		// to avoid duplicates
		removeInputs();
		removeOutputs();

		// FIXME: Replace with your input and output port definitions
		
		// Expects File or HTTP Reference
		List<Class<? extends ExternalReferenceSPI>> expectedReferences = new ArrayList<Class<? extends ExternalReferenceSPI>>();
		expectedReferences.add(HttpReference.class);
		expectedReferences.add(FileReference.class);
		
		addInput(IN_DATA, 0, false, expectedReferences, null);
		
		addOutput(OUT_REPORT, 0);
		addOutput(OUT_URL, 0);

	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void executeAsynch(final Map<String, T2Reference> inputs,
			final AsynchronousActivityCallback callback) {
		// Don't execute service directly now, request to be run ask to be run
		// from thread pool and return asynchronously
		callback.requestRun(new Runnable() {
			
			public void run() {
				InvocationContext context = callback
						.getContext();
				ReferenceService referenceService = context
						.getReferenceService();
				// Resolve inputs 				
				
				
				String url = null;
				String file = null;
				T2Reference inputRef = inputs.get(IN_DATA);
				Identified identified = referenceService.resolveIdentifier(inputRef, null, context);
				if (identified instanceof ReferenceSet) {
					ReferenceSet referenceSet = (ReferenceSet) identified;
					Set<ExternalReferenceSPI> externalReferences = referenceSet
							.getExternalReferences();
					for (ExternalReferenceSPI externalReference : externalReferences) {
						if (externalReference instanceof HttpReference) {
							HttpReference httpReference = (HttpReference) externalReference;
							url = httpReference.getHttpUrlString();
						}
						if (externalReference instanceof FileReference) {
							FileReference fileReference = (FileReference) externalReference;
							file = fileReference.getFilePath();
						}
					}
				}
				
				
				
				
				String report;
				if (file != null) {
					report = "The file is " + file;
					// TODO: Use the file
					url = new File(file).toURI().toASCIIString();
				} else if (url != null) {
					report = "The URL is " + url;
				} else {
					// In case we require a File, give up now
//					callback.fail("No valid reference found, must be URL or File");
//					return;

					
					// or.. make a file of the given content  (or just get the content directly as a String or byte)
					byte[] bytes = (byte[]) referenceService.renderIdentifier(inputRef, byte[].class, context);
					// FIXME: Use externalReference.openConnection(context) instead to save
					// memory
					File tempFile;
					try {
						tempFile = File.createTempFile("taverna", "tmp");
						FileUtils.writeByteArrayToFile(tempFile, bytes);
					} catch (IOException e) {
						callback.fail("Can't create temporary file");
						return;
					}
					tempFile.deleteOnExit();
										
					report = "The temporary file is " + tempFile.getAbsolutePath();
					url = tempFile.toURI().toASCIIString();
					
				}
				
				
				
				// Register outputs
				T2Reference reportRef = referenceService.register(report, 0, true, context);
				T2Reference urlRef = referenceService.register(url, 0, true, context);

				Map<String, T2Reference> outputs = new HashMap<String, T2Reference>();				
				outputs.put(OUT_REPORT, reportRef);
				
				outputs.put(OUT_URL, urlRef);
				// return map of output data, with empty index array as this is
				// the only and final result (this index parameter is used if
				// pipelining output)
				callback.receiveResult(outputs, new int[0]);
			}
		});
	}

	@Override
	public ExampleActivityConfigurationBean getConfiguration() {
		return this.configBean;
	}

}
