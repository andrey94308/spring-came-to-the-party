package org.orgarchive.archive.user;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Shared admin collection query contract. The role rule is inherited by implementations. */
public interface AdminQuery<F, R> {
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    List<R> list(@RequestParam(name = "filter", required = false) F filter,
                 @RequestParam(name = "debug", defaultValue = "false") boolean debug);
}
