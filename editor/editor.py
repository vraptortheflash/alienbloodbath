#!/usr/bin/python

import os, sys, gtk, gtk.glade, pygtk, gobject, thread, gtk.gdk, cairo


LEVEL_WIDTH = 100
LEVEL_HEIGHT = 100
BRUSHES_ZOOM = 0.5
TILE_SIZE = 64
SCROLL_STEP = TILE_SIZE
ZOOM_STEP = 0.25
MIN_ZOOM = 0.25
MAX_ZOOM = 4
LEFT_KEY = 65361
UP_KEY = 65362
RIGHT_KEY = 65363
DOWN_KEY = 65364


class GTKWindow:
  def __init__(self):
    # Set the Glade file.
    self.gladefile = os.path.join(os.path.dirname(__file__), 'editor.glade')
    self.wTree = gtk.glade.XML(self.gladefile)

    # Connect window signals.
    self.window = self.wTree.get_widget('window')
    if (self.window):
      self.window.connect("destroy", gtk.main_quit)
    self.brushes_widget = self.wTree.get_widget('brushes')
    self.tiles_widget = self.wTree.get_widget('tiles')

    signals = { 'on_brushes_expose_event' : self.BrushesExposeEvent,
                'on_tiles_expose_event' : self.TilesExposeEvent,
                'on_quit_menuitem_activate' : gtk.main_quit,
                'on_menuitem_loadlevel_activate' : self.LoadLevel,
                'on_menuitem_loadbrushes_activate' : self.LoadBrushes,
                'on_menuitem_savelevel_activate' : self.SaveLevel,
                'on_brushes_button_press_event' : self.BrushesClickEvent,
                'on_tiles_button_press_event' : self.TilesClickEvent,
                'on_tiles_scroll_event' : self.TilesScrollEvent,
                'on_window_key_press_event' : self.TilesKeyEvent }
    self.wTree.signal_autoconnect(signals)

    # Misc editor state initialization.
    self._tiles = []
    for n in xrange(0, LEVEL_WIDTH * LEVEL_HEIGHT):
      self._tiles.append(0)
    self._brushes_surface = None
    self._selected_brush = 0
    self._view_x = 0
    self._view_y = 0
    self._view_zoom = 1.0

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

  def LoadLevel(self, widget):
    file_path = self.ChooseOpenFile()
    if file_path:
      raw_tiles = open(file_path).read()
      for tile in xrange(LEVEL_WIDTH * LEVEL_HEIGHT):
        self._tiles[tile] = ord(raw_tiles[tile]) - ord('a')
      self.window.queue_draw()

  def LoadBrushes(self, widget):
    file_path = self.ChooseOpenFile()
    if file_path:
      self._brushes_surface = cairo.ImageSurface.create_from_png(file_path)
      self.window.queue_draw()

  def SaveLevel(self, widget):
    file_path = self.ChooseSaveFile()
    if file_path:
      level_string = map(lambda tile : '%c' % (tile + ord('a')), self._tiles)
      open(file_path, "w").write(''.join(level_string))

  def BrushesClickEvent(self, widget, event):
    self._selected_brush = int(event.y / BRUSHES_ZOOM / TILE_SIZE)
    self.window.queue_draw()

  def BrushesExposeEvent(self, widget, event):
    # Documentation for drawing using Cairo was found on:
    # http://www.pygtk.org/articles/cairo-pygtk-widgets/cairo-pygtk-widgets.htm
    # Cairo within Python documenation was found on:
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
    world_x, world_y = self.WindowToWorld(event.x, event.y)
    tile_index = self.WorldToTileIndex(world_x, world_y)
    if tile_index == -1:
      return  # Map tile out of bounds.
    self._tiles[tile_index] = self._selected_brush
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
          cairo_context.rectangle(window_x, window_y, zoomed_tile_size, zoomed_tile_size)
          cairo_context.set_source_rgba(0, 0, 0, 1)
          cairo_context.fill()

          tile_id = self._tiles[tile_index]
          if tile_id == 0:
            y += zoomed_tile_size
            continue  # Map tile not visible.

          cairo_context.rectangle(window_x, window_y, zoomed_tile_size, zoomed_tile_size)
          cairo_context.clip()
          cairo_context.scale(self._view_zoom, self._view_zoom)
          cairo_context.translate(
            window_x / self._view_zoom, window_y / self._view_zoom - TILE_SIZE * tile_id)
          cairo_context.set_source(brushes_pattern)
          cairo_context.paint()
          y += zoomed_tile_size
        x += zoomed_tile_size
        y = 0
    else:
      cairo_context = widget.window.cairo_create()
      cairo_context.select_font_face("Sans")
      cairo_context.set_font_size(18)
      cairo_context.move_to(event.area.width / 2, event.area.height / 2)
      cairo_context.show_text("No Tile Set Loaded")
    return False

  def ChooseOpenFile(self):
    chooser = gtk.FileChooserDialog(
      title=None,action=gtk.FILE_CHOOSER_ACTION_OPEN,
      buttons=(gtk.STOCK_CANCEL, gtk.RESPONSE_CANCEL, gtk.STOCK_OPEN, gtk.RESPONSE_OK))
    response = chooser.run()
    result = None
    if response == gtk.RESPONSE_OK:
      result = chooser.get_filename()
    chooser.destroy()
    return result

  def ChooseSaveFile(self):
    chooser = gtk.FileChooserDialog(
      title=None,action=gtk.FILE_CHOOSER_ACTION_SAVE,
      buttons=(gtk.STOCK_CANCEL, gtk.RESPONSE_CANCEL, gtk.STOCK_SAVE, gtk.RESPONSE_OK))
    response = chooser.run()
    result = None
    if response == gtk.RESPONSE_OK:
      result = chooser.get_filename()
    chooser.destroy()
    return result


if __name__ == "__main__":
  gobject.threads_init()
  gtk_window = GTKWindow()
  gtk.main()
