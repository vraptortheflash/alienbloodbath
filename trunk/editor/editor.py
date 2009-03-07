#!/usr/bin/python

import cairo, gobject, gtk, gtk.gdk, gtk.glade, os, pygtk, re, sys, thread


BRUSHES_ZOOM = 0.5
DOWN_KEY = 65364
INITIAL_ZOOM = 0.5
LEFT_KEY = 65361
LEVEL_WIDTH = 100
LEVEL_HEIGHT = 100
MAX_ZOOM = 4
MIN_ZOOM = 0.25
RIGHT_KEY = 65363
SCROLL_STEP = 64
SETTINGS_DIR = os.getenv("HOME") + '/.abb_editor'
LAST_BRUSH_PATH = SETTINGS_DIR + '/last_brush.txt'
LAST_LEVEL_PATH = SETTINGS_DIR + '/last_level.txt'
TILE_SIZE = 64
UP_KEY = 65362
ZOOM_STEP = 0.25


class TriggerEditorWindow:
  def __init__(self, parent_window, triggers_array, trigger_index):
    self._parent_window = parent_window
    self._triggers_array = triggers_array
    self._trigger_index = trigger_index
    self._gladefile = (
        os.path.join(os.path.dirname(__file__), 'trigger_editor.glade'))
    self._tree = gtk.glade.XML(self._gladefile)

    # Set up the initial trigger text.
    self._entry = self._tree.get_widget('entry')
    trigger_text = self._triggers_array[self._trigger_index]
    if trigger_text:
      self._entry.set_text(trigger_text)

    # Connect window signals.
    self._window = self._tree.get_widget('window')
    signals = { 'on_entry_changed' : self.EntryChangedEvent,
                'on_entry_focus_out_event' : self.LoseFocusEvent,
                'on_window_key_press_event' : self.KeyPressEvent }
    self._tree.signal_autoconnect(signals)

  def DestroyWindow(self):
    self._window.destroy()
    self._parent_window.queue_draw()

  def EntryChangedEvent(self, widget):
    trigger_text = widget.get_text()
    if trigger_text == '':
      trigger_text = None
    self._triggers_array[self._trigger_index] = trigger_text

  def LoseFocusEvent(self, widget, event):
    self.DestroyWindow()

  def KeyPressEvent(self, widget, event):
    if event.keyval == 65293 or event.keyval == 65307:  # Escape or enter key.
      self.DestroyWindow()


class EditorWindow:
  def __init__(self):
    self.gladefile = os.path.join(os.path.dirname(__file__), 'editor.glade')
    self.tree = gtk.glade.XML(self.gladefile)

    # Connect window signals.
    self.window = self.tree.get_widget('window')
    if (self.window):
      self.window.connect("destroy", gtk.main_quit)
    self.brushes_widget = self.tree.get_widget('brushes')
    self.tiles_widget = self.tree.get_widget('tiles')

    signals = { 'on_brushes_expose_event' : self.BrushesExposeEvent,
                'on_tiles_expose_event' : self.TilesExposeEvent,
                'on_quit_menuitem_activate' : gtk.main_quit,
                'on_menuitem_loadlevel_activate' : self.LoadLevelMenu,
                'on_menuitem_loadbrushes_activate' : self.LoadBrushesMenu,
                'on_menuitem_savelevel_activate' : self.SaveLevelMenu,
                'on_brushes_button_press_event' : self.BrushesClickEvent,
                'on_tiles_button_press_event' : self.TilesClickEvent,
                'on_tiles_scroll_event' : self.TilesScrollEvent,
                'on_window_key_press_event' : self.TilesKeyEvent }
    self.tree.signal_autoconnect(signals)

    # Misc editor state initialization.
    self._tiles = []
    for n in xrange(0, LEVEL_WIDTH * LEVEL_HEIGHT):
      self._tiles.append(0)
    self._triggers = []
    for n in xrange(0, LEVEL_WIDTH * LEVEL_HEIGHT):
      self._triggers.append(None)
    self._brushes_surface = None
    self._selected_brush = 0
    self._view_x = 0
    self._view_y = 0
    self._view_zoom = INITIAL_ZOOM

    # Load last opened files
    if os.path.exists(LAST_BRUSH_PATH):
      file_path = open(LAST_BRUSH_PATH).readlines()[0].strip()
      if os.path.exists(file_path):
        self.LoadBrushesFromFile(file_path)
    if os.path.exists(LAST_LEVEL_PATH):
      file_path = open(LAST_LEVEL_PATH).readlines()[0].strip()
      if os.path.exists(file_path):
        self.LoadLevelFromFile(file_path)

  def WorldToWindow(self, world_x, world_y):
    window_x = world_x * self._view_zoom - self._view_x
    window_y = world_y * self._view_zoom - self._view_y
    return window_x, window_y

  def WindowToWorld(self, window_x, window_y):
    world_x = (window_x + self._view_x) / self._view_zoom
    world_y = (window_y + self._view_y) / self._view_zoom
    return world_x, world_y

  def WorldToTileIndex(self, world_x, world_y):
    if (world_x < 0 or world_x >= TILE_SIZE * LEVEL_WIDTH or
        world_y < 0 or world_y >= TILE_SIZE * LEVEL_HEIGHT):
      return -1
    return LEVEL_HEIGHT * int(world_x / TILE_SIZE) + int(world_y / TILE_SIZE)

  def LoadLevelFromFile(self, file_path):
    assert 0 == os.system('mkdir -p %s' % SETTINGS_DIR)
    open(LAST_LEVEL_PATH, 'w').write(file_path)

    level_lines = open(file_path).readlines()
    raw_tiles = level_lines[0].strip()
    for tile in xrange(LEVEL_WIDTH * LEVEL_HEIGHT):
      self._tiles[tile] = ord(raw_tiles[tile]) - ord('a')
    self._triggers = []
    for n in xrange(0, LEVEL_WIDTH * LEVEL_HEIGHT):
      self._triggers.append(None)
    trigger_re = re.compile('(\\d+),(\\d+),"([^"]+)"')
    for raw_trigger in level_lines[1:]:
      match = trigger_re.match(raw_trigger.strip())
      assert match
      x = int(match.group(1))
      y = int(match.group(2))
      trigger = match.group(3)
      trigger_index = LEVEL_HEIGHT * x + y
      self._triggers[trigger_index] = trigger

    self.window.queue_draw()

  def LoadLevelMenu(self, widget):
    file_path = self.ChooseOpenFile()
    if file_path:
      self.LoadLevelFromFile(file_path)

  def LoadBrushesFromFile(self, file_path):
    assert 0 == os.system('mkdir -p %s' % SETTINGS_DIR)
    open(LAST_BRUSH_PATH, 'w').write(file_path)

    self._brushes_surface = cairo.ImageSurface.create_from_png(file_path)
    self.window.queue_draw()

  def LoadBrushesMenu(self, widget):
    file_path = self.ChooseOpenFile()
    if file_path:
      self.LoadBrushesFromFile(file_path)

  def SaveLevelMenu(self, widget):
    file_path = self.ChooseSaveFile()

    assert 0 == os.system('mkdir -p %s' % SETTINGS_DIR)
    open(LAST_LEVEL_PATH, 'w').write(file_path)

    if file_path:
      level_string = map(lambda tile : '%c' % (tile + ord('a')), self._tiles)
      level_file = open(file_path, 'w')
      level_file.write(''.join(level_string))
      for x in xrange(0, LEVEL_WIDTH):
        for y in xrange(0, LEVEL_HEIGHT):
          index = LEVEL_HEIGHT * x + y
          trigger_string = self._triggers[index];
          if trigger_string:
            level_file.write('\n%d,%d,"%s"' % (x, y, trigger_string))

  def BrushesClickEvent(self, widget, event):
    self._selected_brush = int(event.y / BRUSHES_ZOOM / TILE_SIZE)
    self.window.queue_draw()

  def BrushesExposeEvent(self, widget, event):
    # Documentation for drawing using Cairo was found on:
    # http://www.pygtk.org/articles/cairo-pygtk-widgets/cairo-pygtk-widgets.htm
    # Cairo within Python documentation was found on:
    # http://www.cairographics.org/pycairo
    if self._brushes_surface:
      cairo_context = widget.window.cairo_create()
      cairo_context.rectangle(
        event.area.x, event.area.y, event.area.width, event.area.height)
      cairo_context.clip()
      cairo_context.scale(BRUSHES_ZOOM, BRUSHES_ZOOM)
      cairo_context.set_source_surface(self._brushes_surface, 0, 0)
      cairo_context.paint()

      cairo_context.rectangle(
        0, TILE_SIZE * self._selected_brush, TILE_SIZE, TILE_SIZE)
      cairo_context.set_line_width(6)
      cairo_context.set_source_rgba(1, 0, 0, 1)
      cairo_context.stroke()
    return False

  def TilesClickEvent(self, widget, event):
    if not self._brushes_surface:
      return

    world_x, world_y = self.WindowToWorld(event.x, event.y)
    tile_index = self.WorldToTileIndex(world_x, world_y)
    if tile_index == -1:
      return  # Map tile out of bounds.
    if event.button == 1:    # Left click.
      self._tiles[tile_index] = self._selected_brush
      self._triggers[tile_index] = None
      self.window.queue_draw()
    elif event.button == 3:  # Right click.
      trigger_editor_window = (
          TriggerEditorWindow(self.window, self._triggers, tile_index))
      self.window.queue_draw()

  def TilesKeyEvent(self, widget, event):
    if event.keyval == LEFT_KEY:
      self._view_x -= SCROLL_STEP * self._view_zoom
    elif event.keyval == RIGHT_KEY:
      self._view_x += SCROLL_STEP * self._view_zoom
    elif event.keyval == UP_KEY:
      self._view_y -= SCROLL_STEP * self._view_zoom
    elif event.keyval == DOWN_KEY:
      self._view_y += SCROLL_STEP * self._view_zoom
    self.window.queue_draw()

  def TilesScrollEvent(self, widget, event):
    old_world_x, old_world_y = self.WindowToWorld(event.x, event.y)
    if event.direction == 0:
      self._view_zoom += ZOOM_STEP
    elif event.direction == 1:
      self._view_zoom -= ZOOM_STEP
    self._view_zoom = max(self._view_zoom, MIN_ZOOM)
    self._view_zoom = min(self._view_zoom, MAX_ZOOM)
    # Zoom around the mouse pointer.
    new_world_x, new_world_y = self.WindowToWorld(event.x, event.y)
    self._view_x += (old_world_x - new_world_x) * self._view_zoom
    self._view_y += (old_world_y - new_world_y) * self._view_zoom
    self.window.queue_draw()

  def TilesExposeEvent(self, widget, event):
    # See comment in BrushesExposeEvent for Cairo documentation.
    if self._brushes_surface:
      cairo_context = widget.window.cairo_create()
      brushes_pattern = cairo.SurfacePattern(self._brushes_surface)
      brushes_pattern.set_filter(cairo.FILTER_FAST)
      zoomed_tile_size = TILE_SIZE * self._view_zoom
      x = 0
      y = 0
      while x <= event.area.width + zoomed_tile_size:
        while y <= event.area.height + zoomed_tile_size:
          world_x, world_y = self.WindowToWorld(x, y)
          tile_index = self.WorldToTileIndex(world_x, world_y)
          if tile_index == -1:
            y += zoomed_tile_size
            continue  # Map tile out of bounds.

          tile_x = TILE_SIZE * int(world_x / TILE_SIZE)
          tile_y = TILE_SIZE * int(world_y / TILE_SIZE)
          window_x, window_y = self.WorldToWindow(tile_x, tile_y)
          cairo_context.identity_matrix()
          cairo_context.reset_clip()
          cairo_context.rectangle(
              window_x, window_y, zoomed_tile_size, zoomed_tile_size)
          cairo_context.set_source_rgba(0, 0, 0, 1)
          cairo_context.fill()

          tile_id = self._tiles[tile_index]
          trigger = self._triggers[tile_index]

          if tile_id != 0:
            cairo_context.save()
            cairo_context.rectangle(
                window_x, window_y, zoomed_tile_size, zoomed_tile_size)
            cairo_context.clip()
            cairo_context.translate(window_x, window_y);
            cairo_context.scale(self._view_zoom, self._view_zoom)
            cairo_context.translate(0, -TILE_SIZE * tile_id)
            cairo_context.set_source(brushes_pattern)
            cairo_context.paint()
            cairo_context.restore()

          if trigger != None:
            half_tile_size = TILE_SIZE / 2
            cairo_context.save()
            cairo_context.translate(window_x, window_y)
            cairo_context.scale(self._view_zoom, self._view_zoom)
            cairo_context.translate(half_tile_size, half_tile_size)
            cairo_context.arc(
                0.0, 0.0, half_tile_size - 10, 0.0, 2.0 * 3.1415926)
            cairo_context.set_source_rgba(1, 1, 0.5, 0.7)
            cairo_context.set_line_width(3)
            cairo_context.stroke()
            cairo_context.restore()

          y += zoomed_tile_size
        x += zoomed_tile_size
        y = 0
    else:
      cairo_context = widget.window.cairo_create()
      cairo_context.select_font_face("Sans")
      cairo_context.set_font_size(18)
      cairo_context.move_to(event.area.width / 2, event.area.height / 2)
      cairo_context.show_text("No tile set loaded.")
    return False

  def ChooseOpenFile(self):
    chooser = gtk.FileChooserDialog(
      title = None,action=gtk.FILE_CHOOSER_ACTION_OPEN,
      buttons = (gtk.STOCK_CANCEL, gtk.RESPONSE_CANCEL, gtk.STOCK_OPEN,
                 gtk.RESPONSE_OK))
    response = chooser.run()
    result = None
    if response == gtk.RESPONSE_OK:
      result = chooser.get_filename()
    chooser.destroy()
    return result

  def ChooseSaveFile(self):
    chooser = gtk.FileChooserDialog(
      title = None,action=gtk.FILE_CHOOSER_ACTION_SAVE,
      buttons = (gtk.STOCK_CANCEL, gtk.RESPONSE_CANCEL, gtk.STOCK_SAVE,
                 gtk.RESPONSE_OK))
    response = chooser.run()
    result = None
    if response == gtk.RESPONSE_OK:
      result = chooser.get_filename()
    chooser.destroy()
    return result


if __name__ == "__main__":
  gobject.threads_init()
  editor_window = EditorWindow()
  gtk.main()
