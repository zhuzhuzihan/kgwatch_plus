package momoi.mod.kgwatch.hook

import com.kugou.android.watch.lite.common.music.entity.KGSong
import momoi.anno.mixin.StaticHook
import momoi.mod.kgwatch.Settings

/**
 * Client-side song drop gate (search/rank response parsers call this per row;
 * true = the song is silently removed from the result list).
 *
 * The watch builds its verdict from server-delivered config blacklists:
 *   - O0: "singer@name" exact pairs (equalsIgnoreCase)
 *   - M0: name contains / regex list
 *   - N0: another equalsIgnoreCase list
 * The server can hand the old watch client a stricter list than the phone
 * app's — that is why some songs (e.g. 恋愛サーキュレーション/恋爱循环) are
 * invisible on the watch while the request itself succeeds (the server
 * returns them; direct probe confirms 480 hits with watch-shaped params).
 *
 * With [Settings.unlockSearch] on, return false (never drop). Off: delegate
 * to the original. ApkMixin rewrites the c.b.R(...) call inside this body to
 * the renamed original, preserving the config-list logic when disabled.
 */
@StaticHook(c.a.a.a.a.f.e.c.b::class)
fun R(song: KGSong?): Boolean {
    if (Settings.unlockSearch) {
        return false
    }
    return c.a.a.a.a.f.e.c.b.R(song)
}
