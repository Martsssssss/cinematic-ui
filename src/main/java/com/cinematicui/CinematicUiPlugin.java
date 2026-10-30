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

import com.google.inject.Provides;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.util.HotkeyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Cinematic UI",
	description = "Fade or hide the interface after a delay or with a hotkey, choosing which parts fade and when they come back",
	tags = {"fade", "hide", "interface", "ui", "clean", "minimal", "screenshot", "immersion"}
)
public class CinematicUiPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(CinematicUiPlugin.class);

	// Content type of the widget that draws the 3D game. It must never be faded or hidden.
	private static final int VIEWPORT_CONTENT_TYPE = 1337;

	// How long an element stays awake after the mouse leaves it.
	private static final long HOVER_LINGER_MS = 1000;

	// Ticks after the last hitsplat that still count as being in combat.
	private static final int COMBAT_TIMEOUT_TICKS = 10;

	// A click this soon after the window regains focus doesn't wake the interface.
	private static final long FOCUS_CLICK_GRACE_MS = 300;

	// How long the mouse must rest on an element before it wakes, so passing over it doesn't count.
	private static final long HOVER_DWELL_MS = 200;

	// How often the hover area of each element is measured.
	private static final long BOUNDS_REFRESH_MS = 200;

	private static final int[] FORCE_VISIBLE_INTERFACES = {
		InterfaceID.Bankmain.UNIVERSE,
		InterfaceID.BankDepositbox.UNIVERSE,
		InterfaceID.Trademain.WHOLESCREEN,
		InterfaceID.Tradeconfirm.UNIVERSE,
		InterfaceID.GeOffers.UNIVERSE,
		InterfaceID.GeCollect.UNIVERSE,
		InterfaceID.Shopmain.UNIVERSE,
	};

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private CinematicUiConfig config;

	@Inject
	private KeyManager keyManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private PluginManager pluginManager;

	// Plugins this plugin has switched off. Only touched on the Swing thread.
	private final Set<Plugin> stoppedPlugins = new HashSet<>();

	private final Map<UiElement, ElementState> elements = new EnumMap<>(UiElement.class);

	// Written from the input threads, read on the client thread.
	private volatile long lastActivity;
	private volatile boolean hotkeyFaded;
	private volatile boolean idleFaded;
	private volatile boolean faded;
	private volatile boolean mouseInCanvas = true;
	private volatile boolean mouseReentered;
	private volatile long focusGained;

	private long lastFrame;
	private boolean lowStats;
	private boolean wasActive;
	private boolean wasGlobalWake = true;
	private int combatTicks;
	private long hiddenSince;
	private String lastChatInput = "";
	private long lastPrivateMessage;
	private long lastChatTyped;
	private volatile boolean pluginsHidden;

	// Cached config, refreshed on change.
	private long idleDelayMs;
	private float fadeAmount;
	private int fadeMillis;
	private CinematicUiConfig.HoverWake hoverWake;
	private int hpThreshold;
	private int prayerThreshold;
	private boolean showInCombat;
	private boolean showInActivities;
	private boolean showInWilderness;
	private boolean showChatOnPm;
	private long chatTypingWakeMs;
	private volatile List<String> hiddenPluginNames = new ArrayList<>();
	private final Map<UiElement, Boolean> fadeable = new EnumMap<>(UiElement.class);

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (faded)
			{
				hotkeyFaded = false;
				idleFaded = false;
				lastActivity = System.currentTimeMillis();
			}
			else
			{
				hotkeyFaded = true;
			}
		}
	};

	private final KeyListener activityKeyListener = new KeyListener()
	{
		@Override
		public void keyTyped(KeyEvent e)
		{
		}

		@Override
		public void keyPressed(KeyEvent e)
		{
			Keybind hotkey = config.hotkey();
			if (hotkey != Keybind.NOT_SET && hotkey.matches(e))
			{
				return;
			}
			// A keypress always counts as activity, so it also ends an idle fade
			idleFaded = false;
			lastActivity = System.currentTimeMillis();
		}

		@Override
		public void keyReleased(KeyEvent e)
		{
		}
	};

	private final MouseAdapter activityMouseListener = new MouseAdapter()
	{
		@Override
		public MouseEvent mouseEntered(MouseEvent e)
		{
			mouseInCanvas = true;
			mouseReentered = true;
			return e;
		}

		@Override
		public MouseEvent mouseExited(MouseEvent e)
		{
			// The game keeps the last position after the mouse leaves, so remember that it's gone
			mouseInCanvas = false;
			return e;
		}

		@Override
		public MouseEvent mouseMoved(MouseEvent e)
		{
			if (!mouseInCanvas)
			{
				mouseReentered = true;
			}
			mouseInCanvas = true;
			mouseActivity();
			return e;
		}

		@Override
		public MouseEvent mouseDragged(MouseEvent e)
		{
			mouseActivity();
			return e;
		}

		@Override
		public MouseEvent mousePressed(MouseEvent e)
		{
			if (System.currentTimeMillis() - focusGained < FOCUS_CLICK_GRACE_MS)
			{
				// This click is just bringing the window back into focus
				return e;
			}

			// A click anywhere ends the fade, however it started
			hotkeyFaded = false;
			idleFaded = false;
			lastActivity = System.currentTimeMillis();
			return e;
		}
	};

	@Provides
	CinematicUiConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CinematicUiConfig.class);
	}

	@Override
	protected void startUp()
	{
		loadConfig();
		for (UiElement element : UiElement.values())
		{
			elements.put(element, new ElementState());
		}
		hotkeyFaded = false;
		idleFaded = false;
		faded = false;
		lastActivity = System.currentTimeMillis();
		lastFrame = 0;
		wasActive = false;
		wasGlobalWake = true;
		combatTicks = 0;
		hiddenSince = 0;
		lastChatInput = "";
		lastChatTyped = 0;
		lastPrivateMessage = 0;
		pluginsHidden = false;
		keyManager.registerKeyListener(hotkeyListener);
		keyManager.registerKeyListener(activityKeyListener);
		mouseManager.registerMouseListener(activityMouseListener);
	}

	@Override
	protected void shutDown()
	{
		keyManager.unregisterKeyListener(hotkeyListener);
		keyManager.unregisterKeyListener(activityKeyListener);
		mouseManager.unregisterMouseListener(activityMouseListener);
		clientThread.invoke(() ->
		{
			restoreAll();
			elements.clear();
		});
		pluginsHidden = false;
		SwingUtilities.invokeLater(this::startStoppedPlugins);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CinematicUiConfig.GROUP.equals(event.getGroup()))
		{
			loadConfig();
			if (idleDelayMs <= 0)
			{
				idleFaded = false;
			}
			lastActivity = System.currentTimeMillis();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		lowStats = isLowStats();

		if (combatTicks > 0)
		{
			combatTicks--;
		}

		Player local = client.getLocalPlayer();
		if (local != null)
		{
			Actor target = local.getInteracting();
			if (target instanceof NPC && ((NPC) target).getCombatLevel() > 0)
			{
				combatTicks = COMBAT_TIMEOUT_TICKS;
			}
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Actor actor = event.getActor();
		if (actor != null && (actor == client.getLocalPlayer() || event.getHitsplat().isMine()))
		{
			combatTicks = COMBAT_TIMEOUT_TICKS;
		}
	}

	@Subscribe
	public void onFocusChanged(FocusChanged event)
	{
		if (event.isFocused())
		{
			focusGained = System.currentTimeMillis();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();
		if (showChatOnPm && (type == ChatMessageType.PRIVATECHAT || type == ChatMessageType.MODPRIVATECHAT))
		{
			lastPrivateMessage = System.currentTimeMillis();
		}
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		// Someone switched a plugin back on by hand, so it is no longer ours to restart
		if (event.isLoaded())
		{
			stoppedPlugins.remove(event.getPlugin());
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		long now = System.currentTimeMillis();
		long dt = lastFrame == 0 ? 0 : Math.min(now - lastFrame, 250);
		lastFrame = now;

		boolean modern;
		int toplevel = client.getTopLevelInterfaceId();
		if (client.getGameState() == GameState.LOGGED_IN && client.isResized()
			&& (toplevel == InterfaceID.TOPLEVEL_OSRS_STRETCH || toplevel == InterfaceID.TOPLEVEL_PRE_EOC))
		{
			modern = toplevel == InterfaceID.TOPLEVEL_PRE_EOC;
		}
		else
		{
			// Fixed mode, the login screen or a layout this plugin doesn't know
			if (wasActive)
			{
				restoreAll();
				wasActive = false;
			}
			faded = false;
			wasGlobalWake = true;
			updatePlugins(false, now);
			return;
		}
		wasActive = true;

		boolean interacting = isForceVisibleOpen();
		if (interacting)
		{
			// Banking or trading counts as activity for the idle timer
			idleFaded = false;
			lastActivity = now;
		}

		updateFadeState(now);
		trackChatTyping(now);
		boolean inCombat = showInCombat && combatTicks > 0;
		boolean globalWake = !faded || lowStats || interacting || inCombat || isDialogueOpen()
			|| (showInActivities && isActivityHudOpen()) || (showInWilderness && isInWilderness());
		updatePlugins(!globalWake, now);

		Point mouse = mouseInCanvas ? client.getMouseCanvasPosition() : null;
		boolean reentered = mouseReentered;
		mouseReentered = false;
		boolean hoverAny = false;
		for (Map.Entry<UiElement, ElementState> entry : elements.entrySet())
		{
			ElementState state = entry.getValue();
			boolean inside = state.bounds != null && mouse != null && state.bounds.contains(mouse.getX(), mouse.getY());
			if ((wasGlobalWake && !globalWake) || reentered)
			{
				// The fade is starting. A mouse that is already resting on an element, such as after
				// clicking the minimap to walk, doesn't count as hovering until it leaves and comes back.
				state.hoverBlocked = inside;
			}
			else if (!inside)
			{
				state.hoverBlocked = false;
			}
			boolean over = inside && !state.hoverBlocked;
			if (!over)
			{
				state.hoverStart = 0;
			}
			else if (state.hoverStart == 0)
			{
				state.hoverStart = now;
			}
			state.hovered = over && now - state.hoverStart >= HOVER_DWELL_MS;
			if (state.hovered)
			{
				state.lastHover = now;
			}
			if (isFadeable(entry.getKey()) && now - state.lastHover < HOVER_LINGER_MS)
			{
				hoverAny = true;
			}
		}
		wasGlobalWake = globalWake;

		for (Map.Entry<UiElement, ElementState> entry : elements.entrySet())
		{
			UiElement element = entry.getKey();
			ElementState state = entry.getValue();

			boolean wake = globalWake || !isFadeable(element);
			if (!wake)
			{
				if (hoverWake == CinematicUiConfig.HoverWake.EVERYTHING)
				{
					wake = hoverAny;
				}
				else
				{
					wake = now - state.lastHover < HOVER_LINGER_MS;
				}
			}
			if (!wake && element == UiElement.CHATBOX)
			{
				wake = isChatInputOpen() || now - lastChatTyped < chatTypingWakeMs
					|| now - lastPrivateMessage < chatTypingWakeMs;
			}
			if (!wake && element == UiElement.SPLIT_CHAT)
			{
				wake = now - lastPrivateMessage < chatTypingWakeMs;
			}

			advance(state, wake ? 0f : 1f, dt);
			apply(element, state, modern);
		}
	}

	private void mouseActivity()
	{
		// Mouse movement keeps the interface awake, but doesn't end a fade that has already started
		if (!faded)
		{
			lastActivity = System.currentTimeMillis();
		}
	}

	private void updateFadeState(long now)
	{
		boolean useIdle = idleDelayMs > 0;
		if (useIdle && !idleFaded && now - lastActivity >= idleDelayMs)
		{
			idleFaded = true;
		}

		faded = hotkeyFaded || (useIdle && idleFaded);
	}

	private void advance(ElementState state, float target, long dt)
	{
		if (fadeMillis <= 0)
		{
			state.current = target;
			return;
		}
		if (dt <= 0)
		{
			return;
		}

		// Coming back is twice as quick as fading away, so hovering feels responsive
		float duration = target < state.current ? fadeMillis / 2f : fadeMillis;
		float step = dt / duration;
		if (target > state.current)
		{
			state.current = Math.min(target, state.current + step);
		}
		else
		{
			state.current = Math.max(target, state.current - step);
		}
	}

	private void apply(UiElement element, ElementState state, boolean modern)
	{
		float strength = state.current * fadeAmount;
		if (strength <= 0f)
		{
			if (state.touched)
			{
				restore(state);
			}
			cacheBounds(element, state, modern);
			return;
		}

		boolean hide = strength >= 1f;
		if (hide && state.touched && isFullyHidden(element, state, modern))
		{
			// Nothing inside a hidden container is drawn, so there is no need to walk it
			return;
		}

		state.touched = true;
		state.generation++;

		for (int componentId : element.components(modern))
		{
			Widget widget = client.getWidget(componentId);
			if (widget == null)
			{
				continue;
			}

			boolean containsViewport = fadeTree(widget, strength, state);
			setHidden(widget, hide && !containsViewport, state);
		}

		state.opacity.values().removeIf(o -> o.generation != state.generation);
		cacheBounds(element, state, modern);
	}

	private boolean isFullyHidden(UiElement element, ElementState state, boolean modern)
	{
		boolean any = false;
		for (int componentId : element.components(modern))
		{
			Widget widget = client.getWidget(componentId);
			if (widget == null)
			{
				continue;
			}

			HiddenState h = state.hidden.get(widget);
			if (h == null || !h.written || !widget.isSelfHidden())
			{
				return false;
			}
			any = true;
		}
		return any;
	}

	/**
	 * Fades a widget and everything inside it.
	 *
	 * @return true if the 3D game view is inside this widget
	 */
	private boolean fadeTree(Widget widget, float strength, ElementState state)
	{
		if (widget.getContentType() == VIEWPORT_CONTENT_TYPE)
		{
			return true;
		}

		OpacityState o = state.opacity.get(widget);
		int current = widget.getOpacity();
		if (o == null)
		{
			o = new OpacityState();
			o.original = current;
			state.opacity.put(widget, o);
		}
		else if (current != o.written)
		{
			// The game changed it since our last write, so that is the new original
			o.original = current;
		}

		int target = o.original + Math.round((255 - o.original) * strength);
		if (target != current)
		{
			widget.setOpacity(target);
		}
		o.written = target;
		o.generation = state.generation;

		boolean viewport = false;
		viewport |= fadeChildren(widget.getStaticChildren(), strength, state);
		viewport |= fadeChildren(widget.getDynamicChildren(), strength, state);
		viewport |= fadeChildren(widget.getNestedChildren(), strength, state);
		return viewport;
	}

	private boolean fadeChildren(Widget[] children, float strength, ElementState state)
	{
		if (children == null)
		{
			return false;
		}

		boolean viewport = false;
		for (Widget child : children)
		{
			if (child != null)
			{
				viewport |= fadeTree(child, strength, state);
			}
		}
		return viewport;
	}

	private void setHidden(Widget widget, boolean hide, ElementState state)
	{
		HiddenState h = state.hidden.get(widget);
		boolean current = widget.isSelfHidden();
		if (h == null)
		{
			h = new HiddenState();
			h.original = current;
			h.written = current;
			state.hidden.put(widget, h);
		}
		else if (current != h.written)
		{
			h.original = current;
		}

		// Never show something the game itself has hidden
		boolean target = hide || h.original;
		if (target != current)
		{
			widget.setHidden(target);
		}
		h.written = target;
	}

	private void restore(ElementState state)
	{
		for (Map.Entry<Widget, HiddenState> entry : state.hidden.entrySet())
		{
			Widget widget = entry.getKey();
			HiddenState h = entry.getValue();
			if (widget.isSelfHidden() == h.written && h.written != h.original)
			{
				widget.setHidden(h.original);
			}
		}

		for (Map.Entry<Widget, OpacityState> entry : state.opacity.entrySet())
		{
			Widget widget = entry.getKey();
			OpacityState o = entry.getValue();
			if (widget.getOpacity() == o.written && o.written != o.original)
			{
				widget.setOpacity(o.original);
			}
		}

		state.hidden.clear();
		state.opacity.clear();
		state.touched = false;
	}

	private void restoreAll()
	{
		for (ElementState state : elements.values())
		{
			restore(state);
			state.current = 0f;
			state.bounds = null;
			state.lastBoundsUpdate = 0;
		}
	}

	private void cacheBounds(UiElement element, ElementState state, boolean modern)
	{
		long now = System.currentTimeMillis();
		if (now - state.lastBoundsUpdate < BOUNDS_REFRESH_MS)
		{
			return;
		}
		state.lastBoundsUpdate = now;

		// While an element is hidden its bounds are kept from when it was last visible,
		// so hovering over the empty space can still bring it back
		Rectangle union = null;
		for (int componentId : element.components(modern))
		{
			Widget widget = client.getWidget(componentId);
			if (widget == null || widget.isHidden())
			{
				continue;
			}

			Rectangle bounds = visibleBounds(widget);
			if (bounds != null)
			{
				union = union == null ? bounds : union.union(bounds);
			}
		}

		if (union != null)
		{
			state.bounds = union;
		}
		else if (!state.touched)
		{
			state.bounds = null;
		}
	}

	/**
	 * Top level containers often span the whole screen, so the hover area is built
	 * from the visible pieces inside them instead.
	 */
	private Rectangle visibleBounds(Widget root)
	{
		Rectangle union = null;
		Widget[][] groups = {root.getStaticChildren(), root.getNestedChildren()};
		for (Widget[] group : groups)
		{
			if (group == null)
			{
				continue;
			}

			for (Widget child : group)
			{
				if (child == null || child.isSelfHidden() || child.getContentType() == VIEWPORT_CONTENT_TYPE)
				{
					continue;
				}

				// Scrolling areas are measured as a whole, since their contents can sit outside them
				Rectangle bounds = hasChildren(child) && child.getScrollHeight() == 0
					? visibleBounds(child)
					: child.getBounds();
				if (bounds != null && bounds.width > 0 && bounds.height > 0 && !coversScreen(bounds))
				{
					union = union == null ? new Rectangle(bounds) : union.union(bounds);
				}
			}
		}

		if (union == null)
		{
			Rectangle own = root.getBounds();
			if (own != null && own.width > 0 && own.height > 0 && !coversScreen(own))
			{
				union = new Rectangle(own);
			}
		}
		return union;
	}

	private static boolean hasChildren(Widget widget)
	{
		Widget[] nested = widget.getNestedChildren();
		Widget[] statics = widget.getStaticChildren();
		return nested != null && nested.length > 0 || statics != null && statics.length > 0;
	}

	private boolean coversScreen(Rectangle bounds)
	{
		return bounds.width >= client.getCanvasWidth() && bounds.height >= client.getCanvasHeight();
	}

	private boolean isFadeable(UiElement element)
	{
		return fadeable.getOrDefault(element, false);
	}

	private boolean isForceVisibleOpen()
	{
		for (int componentId : FORCE_VISIBLE_INTERFACES)
		{
			Widget widget = client.getWidget(componentId);
			if (widget != null && !widget.isHidden())
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Text left sitting in the chatbox shouldn't keep it awake, so only a change to
	 * the text counts as typing.
	 */
	private void trackChatTyping(long now)
	{
		String input = client.getVarcStrValue(VarClientID.CHATINPUT);
		if (input == null)
		{
			input = "";
		}

		if (!input.equals(lastChatInput))
		{
			if (!input.isEmpty())
			{
				lastChatTyped = now;
			}
			lastChatInput = input;
		}
	}

	private boolean isActivityHudOpen()
	{
		for (int componentId : ActivityHuds.COMPONENTS)
		{
			Widget widget = client.getWidget(componentId);
			if (widget != null && !widget.isHidden())
			{
				return true;
			}
		}
		return false;
	}

	private boolean isInWilderness()
	{
		return client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1 || WorldType.isPvpWorld(client.getWorldType());
	}

	private boolean isChatInputOpen()
	{
		Widget mesLayer = client.getWidget(InterfaceID.Chatbox.MES_LAYER);
		return mesLayer != null && !mesLayer.isSelfHidden();
	}

	/**
	 * NPC dialogue, level up messages and the make-X menu all open in the chatbox modal layer.
	 */
	private boolean isDialogueOpen()
	{
		Widget modal = client.getWidget(InterfaceID.Chatbox.CHATMODAL);
		if (modal == null)
		{
			return false;
		}

		Widget[] nested = modal.getNestedChildren();
		return nested != null && nested.length > 0;
	}

	/**
	 * Switches the chosen plugins off once the interface has fully faded, and back on
	 * as soon as it starts to return.
	 */
	private void updatePlugins(boolean uiHidden, long now)
	{
		if (!uiHidden)
		{
			hiddenSince = 0;
			if (pluginsHidden)
			{
				pluginsHidden = false;
				SwingUtilities.invokeLater(this::startStoppedPlugins);
			}
			return;
		}

		if (hiddenSince == 0)
		{
			hiddenSince = now;
		}

		if (!pluginsHidden && now - hiddenSince >= fadeMillis && !hiddenPluginNames.isEmpty())
		{
			pluginsHidden = true;
			List<String> names = hiddenPluginNames;
			SwingUtilities.invokeLater(() -> stopChosenPlugins(names));
		}
	}

	private void stopChosenPlugins(List<String> names)
	{
		if (!pluginsHidden)
		{
			return;
		}

		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (plugin == this || !names.contains(plugin.getName().toLowerCase(Locale.ROOT))
				|| !pluginManager.isPluginActive(plugin))
			{
				continue;
			}

			try
			{
				if (pluginManager.stopPlugin(plugin))
				{
					stoppedPlugins.add(plugin);
				}
			}
			catch (PluginInstantiationException e)
			{
				log.warn("Couldn't stop {}", plugin.getName(), e);
			}
		}
	}

	private void startStoppedPlugins()
	{
		if (pluginsHidden)
		{
			return;
		}

		for (Plugin plugin : new ArrayList<>(stoppedPlugins))
		{
			stoppedPlugins.remove(plugin);
			try
			{
				// startPlugin does nothing if the plugin was switched off in the meantime
				pluginManager.startPlugin(plugin);
			}
			catch (PluginInstantiationException e)
			{
				log.warn("Couldn't start {}", plugin.getName(), e);
			}
		}
	}

	private boolean isLowStats()
	{
		if (hpThreshold > 0)
		{
			int max = client.getRealSkillLevel(Skill.HITPOINTS);
			if (max > 0 && client.getBoostedSkillLevel(Skill.HITPOINTS) * 100 < hpThreshold * max)
			{
				return true;
			}
		}

		if (prayerThreshold > 0 && isAnyPrayerActive())
		{
			int max = client.getRealSkillLevel(Skill.PRAYER);
			if (max > 0 && client.getBoostedSkillLevel(Skill.PRAYER) * 100 < prayerThreshold * max)
			{
				return true;
			}
		}
		return false;
	}

	private boolean isAnyPrayerActive()
	{
		for (Prayer prayer : Prayer.values())
		{
			if (client.getVarbitValue(prayer.getVarbit()) == 1)
			{
				return true;
			}
		}
		return false;
	}

	private void loadConfig()
	{
		idleDelayMs = config.idleDelay() * 1000L;
		fadeAmount = config.fadeAmount() / 100f;
		fadeMillis = config.fadeSpeed().getMillis();
		hoverWake = config.hoverWakes();
		hpThreshold = config.hpThreshold();
		prayerThreshold = config.prayerThreshold();
		showInCombat = config.showInCombat();
		showInActivities = config.showInActivities();
		showInWilderness = config.showInWilderness();
		showChatOnPm = config.showChatOnPm();
		chatTypingWakeMs = config.chatTypingWake() * 1000L;
		List<String> names = new ArrayList<>();
		for (String name : config.hiddenPlugins().split(","))
		{
			String trimmed = name.trim().toLowerCase(Locale.ROOT);
			if (!trimmed.isEmpty())
			{
				names.add(trimmed);
			}
		}
		hiddenPluginNames = names;
		fadeable.put(UiElement.CHATBOX, config.chatbox() == CinematicUiConfig.ElementMode.FADE);
		fadeable.put(UiElement.MINIMAP, config.minimap() == CinematicUiConfig.ElementMode.FADE);
		fadeable.put(UiElement.SIDE_PANEL, config.sidePanel() == CinematicUiConfig.ElementMode.FADE);
		fadeable.put(UiElement.XP_DROPS, config.xpDrops() == CinematicUiConfig.ElementMode.FADE);
		fadeable.put(UiElement.STATUS_ICONS, config.statusIcons() == CinematicUiConfig.ElementMode.FADE);
		fadeable.put(UiElement.SPLIT_CHAT, config.splitChat() == CinematicUiConfig.ElementMode.FADE);
	}

	private static final class ElementState
	{
		// 0 = fully shown, 1 = fully faded (before applying the fade amount)
		float current;
		boolean touched;
		boolean hovered;
		boolean hoverBlocked;
		long hoverStart;
		long lastHover;
		long lastBoundsUpdate;
		int generation;
		Rectangle bounds;
		final Map<Widget, OpacityState> opacity = new IdentityHashMap<>();
		final Map<Widget, HiddenState> hidden = new IdentityHashMap<>();
	}

	private static final class OpacityState
	{
		int original;
		int written;
		int generation;
	}

	private static final class HiddenState
	{
		boolean original;
		boolean written;
	}
}
