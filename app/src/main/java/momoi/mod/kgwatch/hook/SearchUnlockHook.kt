package momoi.mod.kgwatch.hook

import momoi.anno.mixin.StaticHook
import momoi.mod.kgwatch.Settings
import java.util.Map

/**
 * Unlock search/copyright limits BEFORE the request is signed.
 *
 * Why not getGetRequestParams() (previous attempt): the wire signature is
 * computed earlier over the whole parameter map
 * (signature = getSign(sortQuery(map))), so rewriting the serialized string
 * afterwards invalidates the signature and the server rejects every request —
 * hence "empty results with the switch on".
 *
 * The search flow (c.a.a.a.a.b.n.d$a.call) builds the param map, then calls
 * c.a.a.a.a.c.e.b.p(map) to produce the sort-query string that feeds the
 * signature, and only afterwards constructs the RequestPackage. Hooking p()
 * therefore sees every search request's map pre-signature; mutating it here
 * is automatically covered by the signature that is computed right after.
 *
 * Mutations (only when [Settings.unlockSearch] is on, and only for requests
 * that carry the search-only "privilegefilter" key — everything else passes
 * through byte-identical):
 *   - privilegefilter -> "0"  (no server-side privilege trim)
 *   - pagesize        -> "30" (watch hardcodes 10; phone sends 30)
 *
 * ApkMixin mechanics: the original p() gets renamed (p_0) at merge time and
 * the b.p(map) call inside this body is rewritten to it, so the original
 * sort-query logic still runs on the (already mutated) map.
 */
@StaticHook(c.a.a.a.a.c.e.b::class)
fun p(map: MutableMap<String, Any?>): String {
    try {
        if (Settings.unlockSearch && map.containsKey("privilegefilter")) {
            map["privilegefilter"] = "0"
            if (map.containsKey("pagesize")) {
                map["pagesize"] = "30"
            }
        }
    } catch (t: Throwable) {
        // Never break signing for any request.
    }
    return c.a.a.a.a.c.e.b.p(map)
}
