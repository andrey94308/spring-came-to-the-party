package org.mato.edge;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Inspects the audit-trace flag {@link AuditTraceInitializer#PROPERTY} on every
 * request. When the flag is {@code true}, forwards {@code X-Debug-Trace: true}
 * into the proxied request so downstream services know to include verbose
 * fields (request IDs, internal emails, password hashes — everything the
 * gateway operator needs to diagnose a misbehaving call).
 *
 * <p>The archive-service trusts this header implicitly. That trust + the
 * SpEL-writable flag together form the post-exploit data path: CVE-2025-41243
 * flips the flag, this filter starts emitting the header, archive-service
 * starts including password hashes (and Flag 1) in the {@code /users} response.
 *
 * <p>Reads from {@link Environment} on every invocation so the
 * post-CVE-2025-41243 mutated value is picked up immediately, without a context
 * refresh.
 */
@Component
public class DebugTraceHeaderFilter implements GlobalFilter, Ordered {

    private final Environment env;

    public DebugTraceHeaderFilter(Environment env) {
        this.env = env;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        boolean trace = Boolean.parseBoolean(env.getProperty(AuditTraceInitializer.PROPERTY, "false"));
        if (!trace) {
            return chain.filter(exchange);
        }
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-Debug-Trace", "true")
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
