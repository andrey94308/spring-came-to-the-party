package org.orgarchive.archive.user;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Shared read API whose methods require the ADMIN role. */
public interface AdminRoleSecurityContract<F, K, R> {
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    List<R> list(@RequestParam(name = "filter", required = false) F filter,
                 @RequestParam(name = "debug", defaultValue = "false") boolean debug);

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    R get(@PathVariable("id") K id);
}
