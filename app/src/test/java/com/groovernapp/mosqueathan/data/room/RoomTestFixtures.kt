package com.groovernapp.mosqueathan.data.room

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * A repository over a fake DAO, with a clock the test owns.
 *
 * `Dispatchers.Unconfined` rather than the production `Dispatchers.IO`, and that choice is what
 * makes these tests deterministic rather than merely usually right: the repository's eight
 * collectors are what keep its synchronous cache current, and on `IO` they run on another thread,
 * so `getConfig()` immediately after a write would sometimes see the old value and sometimes the
 * new one. Unconfined resumes each collector in the thread that emitted, so by the time a write
 * returns the cache has already moved. No delays, no polling, no flake.
 */
internal fun roomRepository(dao: MosqueDao, now: () -> Long): RoomConfigRepository =
    RoomConfigRepository(dao = dao, scope = CoroutineScope(Dispatchers.Unconfined), now = now)
