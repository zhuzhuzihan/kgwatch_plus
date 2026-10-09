package momoi.mod.kgwatch.hook

import momoi.anno.mixin.Mixin
import momoi.mod.kgwatch.Settings

/**
 * Unlock search/copyright limits at the request-serialization layer.
 *
 * Target: c.a.a.a.a.b.n.n — the concrete song-search RequestPackage
 * (public, instantiated per search; getGetRequestParams() is inherited from
 * AbstractRequestPackage, so overriding here shadows it for search requests
 * only while super.* still resolves to the original implementation).
 *
 * NOTE: the constructor below exists only to satisfy the Kotlin compiler
 * (ApkMixin drops hook constructors at merge time; the target keeps its own).
 * The class carries no fields and no <init> logic, per hook rules.
 *
 * When [Settings.unlockSearch] is on:
 *   - privilegefilter=<n> -> privilegefilter=0 (no server-side privilege trim)
 *   - pagesize=<n>        -> pagesize=30   (watch hardcodes 10; phone sends 30)
 *
 * Off by default: returns the original string verbatim.
 */
@Mixin
class SearchParamsHook(
    params: java.util.Map<Any?, Any?>,
    callback: c.a.a.a.a.b.n.m
) : c.a.a.a.a.b.n.n(params, callback) {

    override fun getGetRequestParams(): String {
        val raw = super.getGetRequestParams()
        if (!Settings.unlockSearch) return raw
        if (!raw.contains("privilegefilter=")) return raw
        var out = raw.replace(Regex("privilegefilter=\\d+"), "privilegefilter=0")
        out = out.replace(Regex("pagesize=\\d+"), "pagesize=30")
        return out
    }
}
