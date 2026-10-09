package momoi.mod.kgwatch.hook

import momoi.anno.mixin.Mixin
import momoi.mod.kgwatch.Settings

/**
 * Unlock search/copyright limits at the request-serialization layer.
 *
 * Target: c.a.a.a.a.f.f.a (base class of all tracker/content RequestPackages;
 * the search package c.a.a.a.a.b.n.n extends it). getGetRequestParams() itself
 * is defined in AbstractRequestPackage and inherited — overriding here shadows
 * it for every subclass, and super.* resolves to the original via invoke-super.
 *
 * When [Settings.unlockSearch] is on, and only for requests that carry a
 * privilegefilter (i.e. song search — other APIs don't have the key, so they
 * pass through untouched):
 *   - privilegefilter=<n> -> privilegefilter=0 (no server-side privilege trim)
 *   - pagesize=<n>        -> pagesize=30   (watch hardcodes 10; phone sends 30)
 *
 * Off by default: returns the original string verbatim.
 */
@Mixin
class SearchParamsHook : c.a.a.a.a.f.f.a() {

    override fun getGetRequestParams(): String {
        val raw = super.getGetRequestParams()
        if (!Settings.unlockSearch) return raw
        if (!raw.contains("privilegefilter=")) return raw
        var out = raw.replace(Regex("privilegefilter=\\d+"), "privilegefilter=0")
        out = out.replace(Regex("pagesize=\\d+"), "pagesize=30")
        return out
    }
}
