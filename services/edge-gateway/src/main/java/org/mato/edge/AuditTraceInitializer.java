package org.mato.edge;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Seeds the audit-trace flag {@code mato.gateway.audit.debug-trace} into Java's
 * {@code System.getProperties()} on startup so that {@link DebugTraceHeaderFilter}
 * can read it on every request and so that the CVE-2025-41243 SpEL exploit has
 * a writable target.
 *
 * <p>The realistic story: this gateway exposes an "audit / debug-trace" mode,
 * controlled by a single boolean property. When the flag is {@code true}, the
 * gateway forwards an {@code X-Debug-Trace} header into the proxied request so
 * downstream services know to include verbose response fields for operator
 * debugging. The misconfig the CTF abuses is that the flag is reachable from
 * Spring's {@code @systemProperties} bean — the canonical Spring Cloud Gateway
 * SpEL injection writes there with
 * {@code #{@systemProperties['mato.gateway.audit.debug-trace']='true'}}.
 *
 * <p>Why systemProperties and not a custom MapPropertySource: Spring Boot's
 * environment class is {@code ApplicationReactiveWebEnvironment}, and SpEL's
 * reflective accessor cannot reach the inherited {@code getPropertySources()}
 * method from that runtime class. The {@code @systemProperties} bean — Java's
 * {@link java.util.Properties} — is writable directly with the indexer
 * accessor, so the exploit works against a real vulnerable Spring Cloud
 * Gateway 4.2.x dependency without us shipping a custom SpEL configuration.
 *
 * <p>Spring's {@code Environment} resolves the property from the
 * highest-priority source that contains it. Among the default sources,
 * {@code systemProperties} sits above {@code systemEnvironment} and the
 * application yml, so a write into systemProperties takes effect on the very
 * next read.
 */
public class AuditTraceInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static final String PROPERTY = "mato.gateway.audit.debug-trace";

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        String fromEnv = System.getenv("MATO_GATEWAY_AUDIT_DEBUG_TRACE");
        System.setProperty(PROPERTY, fromEnv != null ? fromEnv : "false");
    }
}
