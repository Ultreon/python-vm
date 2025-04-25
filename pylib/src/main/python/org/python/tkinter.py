# from org.python._internal.tkinter import TkNative
#
#
# class Misc:
#     def destroy(self):
#         TkNative.factory().miscDestroy(self)
#
#     def deletecommand(self, name):
#         TkNative.factory().miscDeletecommand(self, name)
#
#     def tk_strictMotif(self, boolean=None):
#         return TkNative.factory().miscTkStrictMotif(self, boolean)
#
#     def tk_bisque(self):
#         TkNative.factory().miscTkBisque(self)
#
#     def tk_setPalette(self, *args, **kw):
#         TkNative.factory().miscTkSetPalette(self, args, kw)
#
#     def tk_focusFollowsMouse(self):
#         TkNative.factory().miscTkFocusFollowsMouse(self)
#
#     def wait_variable(self, name='PY_VAR'):
#         TkNative.factory().miscWaitVariable(self, name)
#
#     def wait_window(self, window=None):
#         TkNative.factory().miscWaitWindow(self, window)
#
#     def wait_visibility(self, window=None):
#         TkNative.factory().miscWaitVisibility(self, window)
#
#     def setvar(self, name='PY_VAR', value='1'):
#         TkNative.factory().miscSetvar(self, name, value)
#
#     def getint(self, s):
#         return TkNative.factory().miscGetint(self, s)
#
#     def getdouble(self, s):
#         return TkNative.factory().miscGetdouble(self, s)
#
#     def getboolean(self, s):
#         return TkNative.factory().miscGetboolean(self, s)
#
#     def focus_set(self):
#         TkNative.factory().miscFocusSet(self)
#
#     def focus_force(self):
#         TkNative.factory().miscFocusForce(self)
#
#     def focus_get(self):
#         return TkNative.factory().miscFocusGet(self)
#
#     def focus_displayof(self, w):
#         return TkNative.factory().miscFocusDisplayof(self, w)
#
#     def focus_lastfor(self, w):
#         return TkNative.factory().miscFocusLastfor(self, w)
#
#     def tk_focusFollowsMouse(self):
#         TkNative.factory().miscTkFocusFollowsMouse(self)
#
#     def tk_focusNext(self):
#         TkNative.factory().miscTkFocusNext(self)
#
#     def tk_focusPrev(self):
#         TkNative.factory().miscTkFocusPrev(self)
#
#     def after(self, ms, func=None, *args):
#         return TkNative.factory().miscAfter(self, ms, func, args)
#
#     def after_idle(self, func, *args):
#         return TkNative.factory().miscAfterIdle(self, func, args)
#
#     def after_cancel(self, id):
#         return TkNative.factory().miscAfterCancel(self, id)
#
#     def clipboard_get(self, **kw):
#         return TkNative.factory().miscClipboardGet(self, kw)
#
#     def clipboard_clear(self, **kw):
#         return TkNative.factory().miscClipboardClear(self, kw)
#
#     def clipboard_append(self, string, **kw):
#         return TkNative.factory().miscClipboardAppend(self, string, kw)
#
#     def grab_current(self):
#         return TkNative.factory().miscGrabCurrent(self)
#
#     def grab_release(self):
#         return TkNative.factory().miscGrabRelease(self)
#
#     def grab_set(self):
#         return TkNative.factory().miscGrabSet(self)
#
#     def grab_set_global(self):
#         return TkNative.factory().miscGrabSetGlobal(self)
#
#     def grab_status(self):
#         return TkNative.factory().miscGrabStatus(self)
#
#     def option_add(self, pattern, value, priority=None):
#         return TkNative.factory().miscOptionAdd(self, pattern, value, priority)
#
#     def option_clear(self):
#         return TkNative.factory().miscOptionClear(self)
#
#     def option_get(self, name, className):
#         return TkNative.factory().miscOptionGet(self, name, className)
#
#     def option_readfile(self, fileName, priority=None):
#         return TkNative.factory().miscOptionReadfile(self, fileName, priority)
#
#     def selection_clear(self):
#         return TkNative.factory().miscSelectionClear(self)
#
#     def selection_get(self, **kw):
#         return TkNative.factory().miscSelectionGet(self, kw)
#
#     def selection_handle(self, command, **kw):
#         return TkNative.factory().miscSelectionHandle(self, command, kw)
#
#     def selection_own(self, **kw):
#         return TkNative.factory().miscSelectionOwn(self, kw)
#
#     def selection_own_get(self, **kw):
#         return TkNative.factory().miscSelectionOwnGet(self, kw)
#
#     def send(self, interp, cmd, *args):
#         return TkNative.factory().miscSend(self, interp, cmd, args)
#
#     def lower(self, belowThis=None):
#         return TkNative.factory().miscLower(self, belowThis)
#
#     def tkraise(self, aboveThis=None):
#         return TkNative.factory().miscTkraise(self, aboveThis)
#
#     def lift(self, aboveThis=None):
#         return TkNative.factory().miscTkraise(self, aboveThis)
#
#     def info_patchlevel(self):
#         return TkNative.factory().miscInfoPatchlevel(self)
#
#     def winfo_atom(self, name, displayof=0):
#         return TkNative.factory().miscWinfoAtom(self, name, displayof)
#
#     def winfo_atomname(self, id, displayof=0):
#         return TkNative.factory().miscWinfoAtomname(self, id, displayof)
#
#     def winfo_cells(self):
#         return TkNative.factory().miscWinfoCells(self)
#
#     def winfo_children(self):
#         return TkNative.factory().miscWinfoChildren(self)
#
#     def winfo_class(self):
#         return TkNative.factory().miscWinfoClass(self)
#
#     def winfo_colormapfull(self):
#         return TkNative.factory().miscWinfoColormapfull(self)
#
#     def winfo_containing(self, rootX, rootY, displayof=0):
#         return TkNative.factory().miscWinfoContaining(self, rootX, rootY, displayof)
#
#     def winfo_depth(self):
#         return TkNative.factory().miscWinfoDepth(self)
#
#     def winfo_exists(self):
#         return TkNative.factory().miscWinfoExists(self)
#
#     def winfo_fpixels(self, number):
#         return TkNative.factory().miscWinfoFpixels(self, number)
#
#     def winfo_geometry(self):
#         return TkNative.factory().miscWinfoGeometry(self)
#
#     def winfo_height(self):
#         return TkNative.factory().miscWinfoHeight(self)
#
#     def winfo_id(self):
#         return TkNative.factory().miscWinfoId(self)
#
#     def winfo_interps(self):
#         return TkNative.factory().miscWinfoInterps(self)
#
#     def winfo_manager(self):
#         return TkNative.factory().miscWinfoManager(self)
#
#     def winfo_name(self):
#         return TkNative.factory().miscWinfoName(self)
#
#     def winfo_parent(self):
#         return TkNative.factory().miscWinfoParent(self)
#
#     def winfo_pathname(self):
#         return TkNative.factory().miscWinfoPathname(self)
#
#     def winfo_pixels(self, number):
#         return TkNative.factory().miscWinfoPixels(self, number)
#
#     def winfo_pointerx(self):
#         return TkNative.factory().miscWinfoPointerx(self)
#
#     def winfo_pointerxy(self):
#         return TkNative.factory().miscWinfoPointerxy(self)
#
#     def winfo_pointery(self):
#         return TkNative.factory().miscWinfoPointery(self)
#
#     def winfo_reqheight(self):
#         return TkNative.factory().miscWinfoReqheight(self)
#
#     def winfo_reqwidth(self):
#         return TkNative.factory().miscWinfoReqwidth(self)
#
#     def winfo_rgb(self, color):
#         return TkNative.factory().miscWinfoRgb(self, color)
#
#     def winfo_rootx(self):
#         return TkNative.factory().miscWinfoRootx(self)
#
#     def winfo_rooty(self):
#         return TkNative.factory().miscWinfoRooty(self)
#
#     def winfo_screen(self):
#         return TkNative.factory().miscWinfoScreen(self)
#
#     def winfo_screencells(self):
#         return TkNative.factory().miscWinfoScreencells(self)
#
#     def winfo_screenheight(self):
#         return TkNative.factory().miscWinfoScreenheight(self)
#
#     def winfo_screenmmheight(self):
#         return TkNative.factory().miscWinfoScreenmmheight(self)
#
#     def winfo_screenmmwidth(self):
#         return TkNative.factory().miscWinfoScreenmmwidth(self)
#
#     def winfo_screenvisual(self):
#         return TkNative.factory().miscWinfoScreenvisual(self)
#
#     def winfo_screenwidth(self):
#         return TkNative.factory().miscWinfoScreenwidth(self)
#
#     def winfo_toplevel(self):
#         return TkNative.factory().miscWinfoToplevel(self)
#
#     def winfo_viewable(self):
#         return TkNative.factory().miscWinfoViewable(self)
#
#     def winfo_visual(self):
#         return TkNative.factory().miscWinfoVisual(self)
#
#     def winfo_visualid(self):
#         return TkNative.factory().miscWinfoVisualid(self)
#
#     def winfo_visualsavailable(self, includeids=False):
#         return TkNative.factory().miscWinfoVisualsavailable(self, includeids)
#
#     def __winfo_parseitem(self, item):
#         return TkNative.factory().miscWinfoParseitem(self, item)
#
#     def __winfogetint(self, item):
#         return TkNative.factory().miscWinfoGetint(self, item)
#
#     def winfo_vrootheight(self):
#         return TkNative.factory().miscWinfoVrootheight(self)
#
#     def winfo_vrootwidth(self):
#         return TkNative.factory().miscWinfoVrootwidth(self)
#
#     def winfo_vrootx(self):
#         return TkNative.factory().miscWinfoVrootx(self)
#
#     def winfo_vrooty(self):
#         return TkNative.factory().miscWinfoVrooty(self)
#
#     def winfo_width(self):
#         return TkNative.factory().miscWinfoWidth(self)
#
#     def winfo_x(self):
#         return TkNative.factory().miscWinfoX(self)
#
#     def winfo_y(self):
#         return TkNative.factory().miscWinfoY(self)
#
#     def update(self):
#         return TkNative.factory().miscUpdate(self)
#
#     def update_idletasks(self):
#         return TkNative.factory().miscUpdateIdletasks(self)
#
#     def bindtags(self, tagList=None):
#         return TkNative.factory().miscBindtags(self, tagList)
#
#     def _bind(self, what, sequence, func, add, needcleanup=1):
#         return TkNative.factory().miscBind0(self, what, sequence, func, add, needcleanup)
#
#     def bind(self, sequence=None, func=None, add=None):
#         return TkNative.factory().miscBind(self, sequence, func, add)
#
#     def unbind(self, sequence, funcid=None):
#         return TkNative.factory().miscUnbind(self, sequence, funcid)
#
#     def _unbind(self, what, funcid=None):
#         return TkNative.factory().miscUnbind0(self, what, funcid)
#
#     def bind_all(self, sequence=None, func=None, add=None):
#         return TkNative.factory().miscBindAll(self, sequence, func, add)
#
#     def unbind_all(self, sequence):
#         return TkNative.factory().miscUnbindAll(self, sequence)
#
#     def bind_class(self, className, sequence=None, func=None, add=None):
#         return TkNative.factory().miscBindClass(self, className, sequence, func, add)
#
#     def unbind_class(self, className, sequence):
#         return TkNative.factory().miscUnbindClass(self, className, sequence)
#
#     def mainloop(self, n=0):
#         return TkNative.factory().miscMainloop(self, n)
#
#     def quit(self):
#         return TkNative.factory().miscQuit(self)
#
#     def _getints(self, string):
#         return TkNative.factory().miscGetints0(self, string)
#
#     def _getdoubles(self, string):
#         return TkNative.factory().miscGetdoubles0(self, string)
#
#     def _getboolean(self, string):
#         return TkNative.factory().miscGetboolean0(self, string)
#
#     def _displayof(self, displayof):
#         return TkNative.factory().miscDisplayof0(self, displayof)
#
#     def _windowingsystem(self):
#         return TkNative.factory().miscWindowingsystem0(self)
#
#     def _options(self, kw):
#         return TkNative.factory().miscOptions0(self, kw)
#
#     def nametowidget(self, name):
#         return TkNative.factory().miscNametowidget0(self, name)
#
#     def _register(self, func, subst=None, needcleanup=1):
#         return TkNative.factory().miscRegister0(self, func, subst, needcleanup)
#
#     def register(self, func, name=None, subst=None):
#         return TkNative.factory().miscRegister(self, func, name, subst)
#
#     def _root(self):
#         return TkNative.factory().miscRoot0(self)
#
#     def _substitude(self, *args):
#         return TkNative.factory().miscSubstitude0(self, args)
#
#     def _report_exception(self, *args):
#         return TkNative.factory().miscReportException0(self, args)
#
#     def _getconfigure(self, *args):
#         return TkNative.factory().miscGetconfigure0(self, args)
#
#     def _getconfigure1(self, *args):
#         return TkNative.factory().miscGetconfigure10(self, args)
#
#     def _configure(self, cmd, cnf, kw):
#         return TkNative.factory().miscConfigure0(self, cmd, cnf, kw)
#
#     def configure(self, cnf=None, **kw):
#         return TkNative.factory().miscConfigure(self, cnf, kw)
#
#     def config(self, cnf=None, **kw):
#         return TkNative.factory().miscConfigure(self, cnf, kw)
#
#     def cget(self, key):
#         return TkNative.factory().miscCget0(self, key)
#
#     def __getitem__(self, key):
#         return TkNative.factory().miscCget0(self, key)
#
#     def __setitem__(self, key, value):
#         return self.configure({key:value})
#
#     def keys(self):
#         return TkNative.factory().miscKeys(self)
#
#     def __str__(self):
#         return TkNative.factory().miscToString(self)
#
#     def __repr__(self):
#         return TkNative.factory().miscRepresent(self)
#
#     _noarg_ = ['_noarg_']
#
#     def pack_propagate(self, flag=_noarg_):
#         return TkNative.factory().miscPackPropagate(self, flag)
#
#     def propogate(self, flag=_noarg_):
#         return TkNative.factory().miscPackPropagate(self, flag)
#
#     def pack_slaves(self):
#         return TkNative.factory().miscPackSlaves(self)
#
#     def slaves(self):
#         return TkNative.factory().miscPackSlaves(self)
#
#     def place_slaves(self):
#         return TkNative.factory().miscPlaceSlaves(self)
#
#     def grid_anchor(self, anchor=None):
#         return TkNative.factory().miscGridAnchor(self, anchor)
#
#     def anchor(self, anchor=None):
#         return TkNative.factory().miscGridAnchor(self, anchor)
#
#     def grid_bbox(self, column=None, row=None, col2=None, row2=None):
#         return TkNative.factory().miscGridBbox(self, column, row, col2, row2)
#
#     def bbox(self, column=None, row=None, col2=None, row2=None):
#         return TkNative.factory().miscGridBbox(self, column, row, col2, row2)
#
#
# class Tk(Misc):
#     def __init__(self):
#         pass
#         # self.__native = TkNative.factory().create()
#
#     def mainloop(self):
#         self.__native.mainloop()
#
#     def destroy(self):
#         self.__native.destroy()
#
#     def update(self):
#         self.__native.update()
#
#     def update_idletasks(self):
#         self.__native.update_idletasks()
#
#     def quit(self):
#         self.__native.quit()
