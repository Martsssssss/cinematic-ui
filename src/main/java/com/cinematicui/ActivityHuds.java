/*
 * Copyright (c) 2026, Marts
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.cinematicui;

import net.runelite.api.gameval.InterfaceID;

/**
 * Minigame and activity displays. While any of these is on screen, the interface stays visible.
 * Most of this list matches the activity displays RuneLite itself lets you move around.
 */
final class ActivityHuds
{
	static final int[] COMPONENTS = {
		InterfaceID.AgilityarenaOverlay.CONTENT,
		InterfaceID.BarbassaultOverHeal.CONTENT_BOTTOM,
		InterfaceID.BarbassaultOverRecruitPlayerNames.CONTENT,
		InterfaceID.BarrowsOverlay.UNIVERSE,
		InterfaceID.BlastFurnaceHud.UNIVERSE,
		InterfaceID.BoardgamesDraughtsOverlay.CONTENT,
		InterfaceID.BoardgamesRunelinkOverlay.CONTENT,
		InterfaceID.BoardgamesRunesquaresOverlay.CONTENT,
		InterfaceID.BoardgamesRuneversiOverlay.CONTENT,
		InterfaceID.BrLobbyoverlay.CONTENT,
		InterfaceID.BrOverlay.CONTENT,
		InterfaceID.BrewOverlay.CONTENT_RIGHT,
		InterfaceID.BrewWaitingRoomOverlay.CONTENTS,
		InterfaceID.CastlewarsStatusOverlaySaradomin.CASTLEWARS_SARADOMIN_EJECT,
		InterfaceID.CastlewarsStatusOverlaySaradomin.SCORES,
		InterfaceID.CastlewarsStatusOverlaySaradomin.SIDEBOX,
		InterfaceID.CastlewarsStatusOverlaySaradomin.TIMER,
		InterfaceID.CastlewarsStatusOverlayZamorak.CASTLEWARS_ZAMORAK_EJECT,
		InterfaceID.CastlewarsStatusOverlayZamorak.SCORES,
		InterfaceID.CastlewarsStatusOverlayZamorak.SIDEBOX,
		InterfaceID.CastlewarsStatusOverlayZamorak.TIMER,
		InterfaceID.CastlewarsWaitingroom.CONTENT,
		InterfaceID.CataBoss.CATA_ALTARS,
		InterfaceID.ClanwarsHud.UNIVERSE,
		InterfaceID.Dt2Infection.OVERLAY,
		InterfaceID.FlamtaerStatus.TEMPLE_LAYER,
		InterfaceID.FossilUnderwater.OXYGEN_LAYER,
		InterfaceID.FossilVolcanicMine.MAIN_INFO,
		InterfaceID.FossilVolcanicMine.VENT_CHAMBERS_INFO,
		InterfaceID.GauntletOverlay.CONTENT,
		InterfaceID.GiantsFoundryHud.FRAME_RECT0,
		InterfaceID.Gnomeball.CONTENTS,
		InterfaceID.GodwarsOverlay.KILLS_LAYER,
		InterfaceID.GotrHud.CONTENT,
		InterfaceID.GotrOverlay.UNIVERSE,
		InterfaceID.HallowedOverlay.CONTENT,
		InterfaceID.HosidiusServeryHud.CONTENT,
		InterfaceID.HosidiusStripFarming.PANEL,
		InterfaceID.InfernoHpHud.UNIVERSE,
		InterfaceID.MagictrainingAlchem.CONTENTS,
		InterfaceID.MagictrainingEncha.CONTENT_RIGHT,
		InterfaceID.MagictrainingEncha.CONTENT_TOP,
		InterfaceID.MagictrainingGrave.CONTENT_RIGHT,
		InterfaceID.MagictrainingGrave.CONTENT_TOP,
		InterfaceID.MagictrainingTele.CONTENT_RIGHT,
		InterfaceID.MagictrainingTele.CONTENT_TOP,
		InterfaceID.MmOverlay.UNIVERSE,
		InterfaceID.MotherlodeHud.DATA,
		InterfaceID.NightmareTotems.TOTEMS,
		InterfaceID.NtkOverlay.CONTENT,
		InterfaceID.NzoneGame.UNIVERSE,
		InterfaceID.PestLanderOverlay.INFO,
		InterfaceID.PestStatusOverlay.CONTENT_LEFT,
		InterfaceID.PestStatusOverlay.CONTENT_RIGHT,
		InterfaceID.PmoonHud.CONTENT,
		InterfaceID.PvpArenaHud.UNIVERSE,
		InterfaceID.RaidsOverlay.DATA,
		InterfaceID.SailingBtHud.UNIVERSE,
		InterfaceID.Sanity.OVERLAY,
		InterfaceID.TemporossHud.STATUS,
		InterfaceID.TemporossLobbyHud.CONTENT,
		InterfaceID.TfaHud.UNIVERSE,
		InterfaceID.ToaHud.CONTENT,
		InterfaceID.ToaLobby.CONTENT,
		InterfaceID.TobHud.NAMES_CONTAINER,
		InterfaceID.TobHud.PROGRESS_CONTAINER,
		InterfaceID.TobHud.STATUS_CONTAINER,
		InterfaceID.TrawlerOverlay.UNIVERSE,
		InterfaceID.TzhaarFightpit.UNIVERSE,
		InterfaceID.WintStatus.UNIVERSE,
	};

	private ActivityHuds()
	{
	}
}
