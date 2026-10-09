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
 *   - filter keeps its original value — server accepts 0..N identically
 *     (verified by direct probe); the sensitive-word precheck never rewrites
 *     filter/privilegefilter, it only short-circuits to an empty result page.
 *     With the precheck bypassed the original value is harmless either way.
 *   - keyword: also add a raw (unfiltered) variant — see below.
 *
 * Keyword re-injection: the watch precheck (b/n/r/b wordlists in res/-e,
 * res/yW) can flag a keyword BEFORE the map reaches here and swap the
 * request to a blocked/empty variant (b/n/p signal). That precheck does not
 * touch this map's keyword, so nothing to undo here — the empty result for
 * keywords like 恋爱循环 comes from the RESPONSE side: KGSong fields are
 * FileName(t)/OriSongName(s1)/Suffix(l)/SingerName(q), and the UI match
 * (b.f.e.c.b.R) compares the keyword against singer-name prefix and
 * name-contains on those; a match failure drops the row client-side.
 * The keyword itself is left untouched by this hook.
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
