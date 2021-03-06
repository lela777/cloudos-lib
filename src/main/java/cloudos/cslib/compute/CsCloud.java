package cloudos.cslib.compute;

import cloudos.cslib.compute.instance.CsInstance;
import cloudos.cslib.compute.instance.CsInstanceRequest;
import cloudos.cslib.ssh.CsKeyPair;

import java.io.InputStream;

public interface CsCloud {

    public void init (CsCloudConfig config);

    public CsCloudConfig getConfig();

    public CsInstance newInstance(CsInstanceRequest request) throws Exception;

    public boolean isRunning(CsInstance instance) throws Exception;
    public int teardown(CsInstance instance) throws Exception;

    public String execute(CsInstance instance, String command) throws Exception;
    public boolean scp(CsInstance instance, InputStream in, String remotePath) throws Exception;
    public boolean ssh(CsInstance instance) throws Exception;

    public CsInstance findInstance(String instanceId, String name, CsKeyPair keyPair);

}
