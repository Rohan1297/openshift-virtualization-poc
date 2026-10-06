# VM Hello — OpenShift Virtualization workshop

A minimal Java 17 / Spring Boot 3.5 application: a greeting page, hostname,
application uptime, and a health endpoint. No database or external API needed.
The application runs **inside a Linux virtual machine** hosted by OpenShift
Virtualization. The supplied Service and Route expose that VM's HTTP port.

## 1. Build and run

On a machine with Java 17+ and Maven 3.6.3+ installed, in this project folder:

```bash
mvn clean package
java -jar target/vm-hello-1.0.0.jar
```

Open http://localhost:8080. Stop with Ctrl+C.

For a RHEL/Fedora VM with configured package repositories, install prerequisites:

```bash
sudo dnf install -y java-17-openjdk-devel maven
```

You can build on your laptop and copy just the JAR to the VM using the workshop's
SSH/file-transfer method. The VM then only needs Java 17+, not Maven.
Run `java -jar vm-hello-1.0.0.jar` inside the VM.

If firewalld is active, allow the application port inside the guest:

```bash
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

Verify inside the guest:

```bash
curl http://localhost:8080/actuator/health
curl 'http://localhost:8080/api/hello?name=Rohan'
curl http://localhost:8080/api/info
```

Expected health response: `{"status":"UP"}`.

## 2. Expose the VM using OpenShift

Prerequisites: a running workshop Linux VM, default pod network with masquerade
binding, `oc` login, and permission to edit the VM and create Services/Routes.
Use the workshop VM; no new VM or storage is provisioned by this project.

Run the following on your workstation or workshop terminal, **not inside the guest**.
Replace the namespace and VM name with your actual values:

```bash
oc project YOUR_WORKSHOP_NAMESPACE
oc edit vm YOUR_VM_NAME
```

Add `app: vm-hello` under **spec.template.metadata.labels** (preserve existing
labels). A label on only the top-level VM metadata will not work:

```yaml
spec:
  template:
    metadata:
      labels:
        app: vm-hello
```

Ensure the running VMI/virt-launcher receives this label. If the template change
has not propagated, restart the VM using the console, then restart the Java app.
Use this label on only your demo VM to avoid routing to unrelated VMs.

```bash
oc apply -f openshift/expose.yaml
oc get route vm-hello
oc get endpoints vm-hello
```

Open `https://` followed by the route HOST/PORT hostname. Edge TLS terminates at
the OpenShift router; the router connects to guest port 8080 over HTTP.
If your VM's masquerade network explicitly lists allowed ports, include TCP 8080
there too. Other network bindings may require the workshop's network setup.

## Troubleshooting

- Local guest curl fails: check Java is running and inspect its terminal output.
- Service endpoints empty: verify the running VM template/pod label and namespace.
- Route returns 503: check endpoints, guest firewall, network binding, and port 8080.
- VM reboot stops the app: run the JAR again. For persistence, use the optional service below.
- A custom `SERVER_PORT` must also match the Service targetPort and guest firewall.

## Optional: start automatically after a VM reboot

Place the JAR at `/opt/vm-hello/vm-hello-1.0.0.jar`, owned by a dedicated user
`vmhello`. Create the user and directory first, then copy your built JAR:

```bash
sudo useradd --system --shell /sbin/nologin vmhello
sudo mkdir -p /opt/vm-hello
sudo cp target/vm-hello-1.0.0.jar /opt/vm-hello/
sudo chown -R vmhello:vmhello /opt/vm-hello
```

Copy `vm-hello.service` to
`/etc/systemd/system/`, then run:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now vm-hello
sudo journalctl -u vm-hello -f
```

## Demo flow

1. Open the page through the Route and enter your name.
2. Refresh details to show guest hostname and JVM uptime.
3. Open `/actuator/health` to show the app is healthy.
4. Restart the app: uptime resets. With systemd enabled, it also starts after reboot.

This is an unauthenticated workshop demo. The displayed information describes the
OS/JVM hosting the app; it does not query the OpenShift cluster or detect virtualization.

## References

- https://docs.spring.io/spring-boot/3.5/system-requirements.html
- https://docs.redhat.com/en/documentation/openshift_container_platform/4.21/html/virtualization/networking
