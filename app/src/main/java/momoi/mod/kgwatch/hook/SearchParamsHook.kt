package momoi.mod.kgwatch.hook

import momoi.anno.mixin.Mixin
import momoi.mod.kgwatch.Settings

/**
 * Unlock search/copyright limits at the request-serialization layer.
 *
 * Target: com.kugou.common.network.protocol.AbstractRequestPackage — the
 * concrete base of every content RequestPackage (search, rank, tracker…).
 * getGetRequestParams() is defined here, so super.* calls the preserved
 * original and the override takes effect for all subclasses via dispatch.
 *
 * When [Settings.unlockSearch] is on, and only for requests that carry a
 * privilegefilter (i.e. song search — other APIs don't have the key, so they
 * pass through untouched apart from one contains() check):
 *   - privilegefilter=<n> -> privilegefilter=0 (no server-side privilege trim)
 *   - pagesize=<n>        -> pagesize=30   (watch hardcodes 10; phone sends 30)
 *
 * Off by default: returns the original string verbatim.
 */
@Mixin
class SearchParamsHook : com.kugou.common.network.protocol.AbstractRequestPackage() {

    override fun getGetRequestParams(): String {
        val raw = super.getGetRequestParams()
        if (!Settings.unlockSearch) return raw
        if (!raw.contains("privilegefilter=")) return raw
        var out = raw.replace(Regex("privilegefilter=\\d+"), "privilegefilter=0")
        out = out.replace(Regex("pagesize=\\d+"), "pagesize=30")
        return out
    }
}
