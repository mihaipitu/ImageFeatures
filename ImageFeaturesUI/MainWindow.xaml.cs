using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Shapes;
using System.Windows.Data;
using System.Windows.Media.Animation;
using System.Xml;
using System.Windows.Media;
using System.Net.Sockets;
using System.IO;
using System.Text;
using System.Net;
using System.Threading;
using System.Windows.Controls;
using System.Globalization;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using System.Collections;
using System.Collections.Generic;

namespace ImageFeaturesUI
{

    public class PredictionsSorter : IComparer
    {
        public int Compare(object x, object y)
        {
            return 0;
        }
    }

    [ValueConversion(typeof(ImageSource), typeof(String))]
    public class UriImageConverter : IMultiValueConverter
    {
        public object Convert(object[] value, Type targetType, object parameter, CultureInfo culture)
        {
            BitmapImage img = null;
            try
            {
                //String file = value[0] as String + value[1] as String + ".jpg";
                String file = "pack://application:,,,/ImageFeaturesUI;component/Images/" + value[1] as String + ".jpg";
                Uri uri = new Uri(file);
                img = new BitmapImage(uri);
            }
            catch { }
            return img;
        }

        public object[] ConvertBack(object value, Type[] targetType, object parameter, CultureInfo culture)
        {
            return null;
        }
    }

    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    internal partial class MainWindow : Window
    {
        private const Int32 WM_SYSCOMMAND = 0x112;

        private static readonly TimeSpan s_doubleClick
            = TimeSpan.FromMilliseconds(500);

        private HwndSource m_hwndSource;
        private DateTime m_headerLastClicked;

        /// <summary>
        /// Initializes a new instance of the <see cref="MainWindow"/> class.
        /// </summary>
        public MainWindow()
        {
            InitializeComponent();
            AppDomain.CurrentDomain.UnhandledException += (delegate(object sender, UnhandledExceptionEventArgs args)
            {
                MessageBox.Show(args.ExceptionObject.ToString());
            });

        }

        /// <summary>
        /// Raises the <see cref="E:System.Windows.FrameworkElement.Initialized"/> event. 
        /// This method is invoked whenever 
        /// <see cref="P:System.Windows.FrameworkElement.IsInitialized"/> is set to true internally.
        /// </summary>
        /// <param name="e">The <see cref="T:System.Windows.RoutedEventArgs"/> 
        /// that contains the event data.</param>
        protected override void OnInitialized(EventArgs e)
        {
            AllowsTransparency = false;
            ResizeMode = ResizeMode.NoResize;
            //Height                = 480;
            //Width                 = 852;  
            WindowStartupLocation = WindowStartupLocation.CenterScreen;

            SourceInitialized += HandleSourceInitialized;

            GotKeyboardFocus += HandleGotKeyboardFocus;
            LostKeyboardFocus += HandleLostKeyboardFocus;

            base.OnInitialized(e);
        }

        /// <summary>
        /// Handles the source initialized.
        /// </summary>
        /// <param name="sender">The sender.</param>
        /// <param name="e">The <see cref="System.EventArgs"/> 
        /// instance containing the event data.</param>
        private void HandleSourceInitialized(Object sender, EventArgs e)
        {
            m_hwndSource = (HwndSource)PresentationSource.FromVisual(this);

            // Returns the HwndSource object for the window
            // which presents WPF content in a Win32 window.
            HwndSource.FromHwnd(m_hwndSource.Handle).AddHook(
                new HwndSourceHook(NativeMethods.WindowProc));

            // http://msdn.microsoft.com/en-us/library/aa969524(VS.85).aspx
            Int32 DWMWA_NCRENDERING_POLICY = 2;
            NativeMethods.DwmSetWindowAttribute(
                m_hwndSource.Handle,
                DWMWA_NCRENDERING_POLICY,
                ref DWMWA_NCRENDERING_POLICY,
                4);

            // http://msdn.microsoft.com/en-us/library/aa969512(VS.85).aspx
            NativeMethods.ShowShadowUnderWindow(m_hwndSource.Handle);
        }

        /// <summary>
        /// Handles the preview mouse move.
        /// </summary>
        /// <param name="sender">The sender.</param>
        /// <param name="e">The <see cref="System.Windows.Input.MouseEventArgs"/> 
        /// instance containing the event data.</param>
        [DebuggerStepThrough]
        private void HandlePreviewMouseMove(Object sender, MouseEventArgs e)
        {
            if (Mouse.LeftButton != MouseButtonState.Pressed)
            {
                Cursor = Cursors.Arrow;
            }
        }

        /// <summary>
        /// Handles the header preview mouse down.
        /// </summary>
        /// <param name="sender">The sender.</param>
        /// <param name="e">The <see cref="System.Windows.Input.MouseButtonEventArgs"/>
        /// instance containing the event data.</param>
        private void HandleHeaderPreviewMouseDown(Object sender, MouseButtonEventArgs e)
        {

            if (DateTime.Now.Subtract(m_headerLastClicked) <= s_doubleClick)
            {
                // Execute the code inside the event handler for the 
                // restore button click passing null for the sender
                // and null for the event args.
                HandleRestoreClick(null, null);
            }

            m_headerLastClicked = DateTime.Now;

            if (Mouse.LeftButton == MouseButtonState.Pressed)
            {
                DragMove();
            }
        }

        /// <summary>
        /// Handles the minimize click.
        /// </summary>
        /// <param name="sender">The sender.</param>
        /// <param name="e">The <see cref="System.Windows.RoutedEventArgs"/> 
        /// instance containing the event data.</param>
        private void HandleMinimizeClick(Object sender, RoutedEventArgs e)
        {
            WindowState = WindowState.Minimized;
        }

        /// <summary>
        /// Handles the restore click.
        /// </summary>
        /// <param name="sender">The sender.</param>
        /// <param name="e">The <see cref="System.Windows.RoutedEventArgs"/> 
        /// instance containing the event data.</param>
        private void HandleRestoreClick(Object sender, RoutedEventArgs e)
        {
            WindowState = (WindowState == WindowState.Normal)
                ? WindowState.Maximized : WindowState.Normal;

            m_frameGrid.IsHitTestVisible
                = WindowState == WindowState.Maximized
                ? false : true;

            m_resize.Visibility = (WindowState == WindowState.Maximized)
                ? Visibility.Hidden : Visibility.Visible;

            m_roundBorder.Visibility = (WindowState == WindowState.Maximized)
                ? Visibility.Hidden : Visibility.Visible;
        }

        /// <summary>
        /// Handles the got keyboard focus.
        /// </summary>
        /// <param name="sender">The sender.</param>
        /// <param name="e">The <see cref="System.Windows.Input.KeyboardFocusChangedEventArgs"/>
        /// instance containing the event data.</param>
        public void HandleGotKeyboardFocus(Object sender, KeyboardFocusChangedEventArgs e)
        {
            m_roundBorder.Visibility = Visibility.Visible;
        }

        /// <summary>
        /// Handles the lost keyboard focus.
        /// </summary>
        /// <param name="sender">The sender.</param>
        /// <param name="e">The <see cref="System.Windows.Input.KeyboardFocusChangedEventArgs"/>
        /// instance containing the event data.</param>
        public void HandleLostKeyboardFocus(Object sender, KeyboardFocusChangedEventArgs e)
        {
            m_roundBorder.Visibility = Visibility.Hidden;
        }

        /// <summary>
        /// Handles the close click.
        /// </summary>
        /// <param name="sender">The sender.</param>
        /// <param name="e">The <see cref="System.Windows.RoutedEventArgs"/> 
        /// instance containing the event data.</param>
        private void HandleCloseClick(Object sender, RoutedEventArgs e)
        {
            Close();
        }

        /// <summary>
        /// Handles the rectangle mouse move.
        /// </summary>
        /// <param name="sender">The sender.</param>
        /// <param name="e">The <see cref="System.Windows.Input.MouseEventArgs"/> 
        /// instance containing the event data.</param>
        private void HandleRectangleMouseMove(Object sender, MouseEventArgs e)
        {
            Rectangle clickedRectangle = (Rectangle)sender;

            switch (clickedRectangle.Name)
            {
                case "top":
                    Cursor = Cursors.SizeNS;
                    break;
                case "bottom":
                    Cursor = Cursors.SizeNS;
                    break;
                case "left":
                    Cursor = Cursors.SizeWE;
                    break;
                case "right":
                    Cursor = Cursors.SizeWE;
                    break;
                case "topLeft":
                    Cursor = Cursors.SizeNWSE;
                    break;
                case "topRight":
                    Cursor = Cursors.SizeNESW;
                    break;
                case "bottomLeft":
                    Cursor = Cursors.SizeNESW;
                    break;
                case "bottomRight":
                    Cursor = Cursors.SizeNWSE;
                    break;

                default:
                    break;
            }
        }

        /// <summary>
        /// Handles the rectangle preview mouse down.
        /// </summary>
        /// <param name="sender">The sender.</param>
        /// <param name="e">The <see cref="System.Windows.Input.MouseButtonEventArgs"/> 
        /// instance containing the event data.</param>
        private void HandleRectanglePreviewMouseDown(Object sender, MouseButtonEventArgs e)
        {
            Rectangle clickedRectangle = (Rectangle)sender;

            switch (clickedRectangle.Name)
            {
                case "top":
                    Cursor = Cursors.SizeNS;
                    ResizeWindow(ResizeDirection.Top);
                    break;
                case "bottom":
                    Cursor = Cursors.SizeNS;
                    ResizeWindow(ResizeDirection.Bottom);
                    break;
                case "left":
                    Cursor = Cursors.SizeWE;
                    ResizeWindow(ResizeDirection.Left);
                    break;
                case "right":
                    Cursor = Cursors.SizeWE;
                    ResizeWindow(ResizeDirection.Right);
                    break;
                case "topLeft":
                    Cursor = Cursors.SizeNWSE;
                    ResizeWindow(ResizeDirection.TopLeft);
                    break;
                case "topRight":
                    Cursor = Cursors.SizeNESW;
                    ResizeWindow(ResizeDirection.TopRight);
                    break;
                case "bottomLeft":
                    Cursor = Cursors.SizeNESW;
                    ResizeWindow(ResizeDirection.BottomLeft);
                    break;
                case "bottomRight":
                    Cursor = Cursors.SizeNWSE;
                    ResizeWindow(ResizeDirection.BottomRight);
                    break;

                default:
                    break;
            }
        }

        /// <summary>
        /// Resizes the window.
        /// </summary>
        /// <param name="direction">The direction.</param>
        private void ResizeWindow(ResizeDirection direction)
        {
            NativeMethods.SendMessage(m_hwndSource.Handle, WM_SYSCOMMAND,
                (IntPtr)(61440 + direction), IntPtr.Zero);
        }

        public enum ResizeDirection
        {
            Left = 1,
            Right = 2,
            Top = 3,
            TopLeft = 4,
            TopRight = 5,
            Bottom = 6,
            BottomLeft = 7,
            BottomRight = 8,
        }

        private sealed class NativeMethods
        {
            [DllImport("dwmapi.dll", PreserveSig = true)]
            internal static extern Int32 DwmSetWindowAttribute(
                IntPtr hwnd,
                Int32 attr,
                ref Int32 attrValue,
                Int32 attrSize);

            [DllImport("dwmapi.dll")]
            internal static extern Int32 DwmExtendFrameIntoClientArea(
                IntPtr hWnd,
                ref MARGINS pMarInset);

            [DllImport("user32")]
            internal static extern Boolean GetMonitorInfo(
                IntPtr hMonitor,
                MONITORINFO lpmi);

            [DllImport("User32")]
            internal static extern IntPtr MonitorFromWindow(
                IntPtr handle,
                Int32 flags);

            [DllImport("user32.dll", CharSet = CharSet.Auto)]
            internal static extern IntPtr SendMessage(
                IntPtr hWnd,
                UInt32 msg,
                IntPtr wParam,
                IntPtr lParam);

            [DebuggerStepThrough]
            internal static IntPtr WindowProc(
                IntPtr hwnd,
                Int32 msg,
                IntPtr wParam,
                IntPtr lParam,
                ref Boolean handled)
            {
                switch (msg)
                {
                    case 0x0024:
                        WmGetMinMaxInfo(hwnd, lParam);
                        handled = true;
                        break;
                }

                return (IntPtr)0;
            }

            internal static void WmGetMinMaxInfo(IntPtr hwnd, IntPtr lParam)
            {
                MINMAXINFO mmi = (MINMAXINFO)Marshal.PtrToStructure(lParam, typeof(MINMAXINFO));

                // Adjust the maximized size and position to fit the work area 
                // of the correct monitor.
                Int32 MONITOR_DEFAULTTONEAREST = 0x00000002;

                IntPtr monitor = MonitorFromWindow(hwnd, MONITOR_DEFAULTTONEAREST);
                if (monitor != IntPtr.Zero)
                {
                    MONITORINFO monitorInfo = new MONITORINFO();
                    GetMonitorInfo(monitor, monitorInfo);

                    RECT rcWorkArea = monitorInfo.m_rcWork;
                    RECT rcMonitorArea = monitorInfo.m_rcMonitor;

                    mmi.m_ptMaxPosition.m_x = Math.Abs(rcWorkArea.m_left - rcMonitorArea.m_left);
                    mmi.m_ptMaxPosition.m_y = Math.Abs(rcWorkArea.m_top - rcMonitorArea.m_top);

                    mmi.m_ptMaxSize.m_x = Math.Abs(rcWorkArea.m_right - rcWorkArea.m_left);
                    mmi.m_ptMaxSize.m_y = Math.Abs(rcWorkArea.m_bottom - rcWorkArea.m_top);
                }

                Marshal.StructureToPtr(mmi, lParam, true);
            }

            internal static void ShowShadowUnderWindow(IntPtr intPtr)
            {
                MARGINS marInset = new MARGINS();
                marInset.m_bottomHeight = -1;
                marInset.m_leftWidth = -1;
                marInset.m_rightWidth = -1;
                marInset.m_topHeight = -1;

                DwmExtendFrameIntoClientArea(intPtr, ref marInset);
            }

            [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Auto)]
            internal sealed class MONITORINFO
            {
                public Int32 m_cbSize;
                public RECT m_rcMonitor;
                public RECT m_rcWork;
                public Int32 m_dwFlags;

                public MONITORINFO()
                {
                    m_cbSize = Marshal.SizeOf(typeof(MONITORINFO));
                    m_rcMonitor = new RECT();
                    m_rcWork = new RECT();
                    m_dwFlags = 0;
                }
            }

            [StructLayout(LayoutKind.Sequential, Pack = 0)]
            internal struct RECT
            {
                public static readonly RECT Empty = new RECT();

                public Int32 m_left;
                public Int32 m_top;
                public Int32 m_right;
                public Int32 m_bottom;

                public RECT(Int32 left, Int32 top, Int32 right, Int32 bottom)
                {
                    m_left = left;
                    m_top = top;
                    m_right = right;
                    m_bottom = bottom;
                }

                public RECT(RECT rcSrc)
                {
                    m_left = rcSrc.m_left;
                    m_top = rcSrc.m_top;
                    m_right = rcSrc.m_right;
                    m_bottom = rcSrc.m_bottom;
                }
            }

            [StructLayout(LayoutKind.Sequential)]
            internal struct MARGINS
            {
                public Int32 m_leftWidth;
                public Int32 m_rightWidth;
                public Int32 m_topHeight;
                public Int32 m_bottomHeight;
            }

            [StructLayout(LayoutKind.Sequential)]
            private struct POINT
            {
                public Int32 m_x;
                public Int32 m_y;

                public POINT(Int32 x, Int32 y)
                {
                    m_x = x;
                    m_y = y;
                }
            }

            [StructLayout(LayoutKind.Sequential)]
            private struct MINMAXINFO
            {
                public POINT m_ptReserved;
                public POINT m_ptMaxSize;
                public POINT m_ptMaxPosition;
                public POINT m_ptMinTrackSize;
                public POINT m_ptMaxTrackSize;
            };
        }

        private bool isImage(String file)
        {
            bool response = false;
            try
            {
                Uri uri = new Uri(file);
                BitmapImage img = new BitmapImage(uri);
                response = true;
            }
            catch { }
            return response;
        }

        private bool usetags = true;
        private string _tagsDir;
        private string tagsDir
        {
            get
            {
                if (usetags || _tagsDir == null)
                {
                    usetags = false;
                    System.Windows.Forms.FolderBrowserDialog dlgf = new System.Windows.Forms.FolderBrowserDialog();
                    dlgf.Description = "Select user tags folder";
                    dlgf.SelectedPath = @"d:\faculta\licenta\Testset2012\test_metadata\tags\";

                    if (dlgf.ShowDialog() == System.Windows.Forms.DialogResult.OK)
                    {
                        _tagsDir = dlgf.SelectedPath;
                        if (!_tagsDir.EndsWith("\\"))
                            _tagsDir += "\\";
                    }
                    else
                    {
                        usetags = false;
                    }
                }
                return _tagsDir;
            }
        }

        private void update(String status)
        {
            this.responseTextBlock.Text = status;
            this.playStoryboard.Begin();
        }

        private string getImageUserTags(string imagePath)
        {
            XmlNodeList images = this.imagesXml.Document.SelectNodes("images/image");
            XmlNode imageNode = null;
            foreach (XmlNode image in images)
            {
                XmlNode path = image.SelectSingleNode("path");
                if (path != null)
                    if (path.InnerText != null)
                        if (path.InnerText.ToLower().Equals(imagePath.ToLower()))
                        {
                            imageNode = image;
                            break;
                        }
            }

            XmlNode tags = imageNode.SelectSingleNode("tags");
            StringBuilder sb = new StringBuilder();
            sb.Append('\t');

            if (tags == null)
            {
                try
                {
                    if (this.tagsDir != "")
                    {
                        XmlNode xmlTags = this.imagesXml.Document.CreateNode(XmlNodeType.Element, "tags", "");
                        StringBuilder sbtags = new StringBuilder();
                        String f = _tagsDir + imageNode.SelectSingleNode("displaypath").InnerText;
                        f = f.Substring(0, f.LastIndexOf('.'));
                        f += ".txt";
                        System.IO.StreamReader file = new System.IO.StreamReader(f);
                        String line;
                        while ((line = file.ReadLine()) != null)
                        {
                            line.Replace("\t", "");
                            sb.Append(line);
                            sbtags.Append(line + ", ");
                            sb.Append('\t');
                        }
                        file.Close();

                        xmlTags.InnerText = sbtags.ToString().Substring(0, sbtags.ToString().Length - 2);
                        imageNode.AppendChild(xmlTags);
                    }
                }
                catch
                {
                    update("Could not read user tags...");
                }
            }


            return sb.ToString();
        }

        private void addToXml(String filename)
        {
            filename = filename.ToLower();
            String xpath = "images/image"; ///*[text()=\'" + filename + "\']";
            XmlNodeList images = this.imagesXml.Document.SelectNodes(xpath);
            XmlNode imageNode = null;
            foreach (XmlNode image in images)
            {
                XmlNode path = image.SelectSingleNode("path");
                if (path != null)
                    if (path.InnerText != null)
                        if (path.InnerText.ToLower().Equals(filename))
                        {
                            imageNode = image;
                            break;
                        }

            }

            if (imageNode == null)
            {
                if (isImage(filename))
                {
                    XmlDocument currentDoc = this.imagesXml.Document;
                    XmlNode imagesNode = currentDoc.SelectSingleNode("images");

                    imageNode = currentDoc.CreateNode(XmlNodeType.Element, "image", "");
                    XmlNode pathNode = currentDoc.CreateNode(XmlNodeType.Element, "path", "");
                    pathNode.InnerText = filename;
                    XmlNode pathDisplayNode = currentDoc.CreateNode(XmlNodeType.Element, "displaypath", "");
                    int i = filename.LastIndexOf('\\');
                    if (i < 0)
                        i = filename.LastIndexOf('/');
                    if (i > 0)
                        pathDisplayNode.InnerText = filename.Substring(i + 1);
                    else
                        pathDisplayNode.InnerText = filename;

                    imageNode.AppendChild(pathNode);
                    imageNode.AppendChild(pathDisplayNode);
                    imagesNode.AppendChild(imageNode);
                }
            }
            this.imagesListBox.ScrollIntoView(imageNode);
            this.imagesListBox.SelectedItem = imageNode;
        }

        private void ClearButton_Click(object sender, RoutedEventArgs e)
        {
            if (MessageBox.Show(this, "Are you sure that you want to clear the images list?", "Clear", MessageBoxButton.YesNo) == MessageBoxResult.Yes)
            {
                XmlNode imagesNode = this.imagesXml.Document.SelectSingleNode("images");
                imagesNode.RemoveAll();
            }
        }

        private void AddButton_Click(object sender, RoutedEventArgs e)
        {
            Microsoft.Win32.OpenFileDialog dlg = new Microsoft.Win32.OpenFileDialog();
            dlg.Multiselect = true;
            dlg.DefaultExt = ".jpg";

            if (dlg.ShowDialog().Value == true)
            {
                foreach (string filename in dlg.FileNames)
                {
                    addToXml(filename);
                    update("Image added.");
                }
            }
        }

        private void AddFolderButton_Click(object sender, RoutedEventArgs e)
        {
            System.Windows.Forms.FolderBrowserDialog dlgf = new System.Windows.Forms.FolderBrowserDialog();

            if (dlgf.ShowDialog() == System.Windows.Forms.DialogResult.OK)
            {
                String dir = dlgf.SelectedPath;
                string[] filePaths = Directory.GetFiles(dir);
                foreach (string file in filePaths)
                {
                    addToXml(file);
                }
                update("Images added.");
            }

        }

        private IDictionary<int, String> _concepts;
        private IDictionary<int, String> concepts
        {
            get
            {
                if (_concepts == null)
                {
                    _concepts = new Dictionary<int, String>();
                    DateTime start = DateTime.Now;
                    String response = Request("concepts ");
                    try
                    {
                        String[] c = response.Split('|');
                        foreach (String strLine in c)
                        {
                            int i = strLine.IndexOf('\t');
                            if (i < 0)
                                i = strLine.IndexOf(' ');
                            int index = int.Parse(strLine.Substring(0, i));
                            String concept = strLine.Substring(i + 1);
                            concepts.Add(index, concept);
                        }
                    }
                    catch
                    {
                        update("Could not parse concepts file from server.");
                    }
                    update("Loading concepts...");

                }

                return _concepts;
            }
        }

        private void AddSubmissionFileButton_Click(object sender, RoutedEventArgs e)
        {
            if (this.lines != null)
            {
                getsubmissions();
                return;
            }
            Microsoft.Win32.OpenFileDialog dlg = new Microsoft.Win32.OpenFileDialog();
            dlg.Title = "Open submission file";
            dlg.DefaultExt = ".txt";

            System.Windows.Forms.FolderBrowserDialog dlgf = new System.Windows.Forms.FolderBrowserDialog();
            dlgf.Description = "Select test images folder";
            dlgf.SelectedPath = @"d:\faculta\licenta\Testset2012\images\";

            if (dlgf.ShowDialog() == System.Windows.Forms.DialogResult.OK)
            {
                if (dlg.ShowDialog().Value == true)
                {
                    if (this.concepts == null)
                    {
                        update("The concepts could not be retrevied from server.");
                        return;
                    }

                    //String imagesDir = @"d:\faculta\licenta\Testset2012\images\";
                    if (this.lines == null)
                    {
                        imagesDir = dlgf.SelectedPath;
                        if (!imagesDir.EndsWith("\\"))
                            imagesDir += "\\";
                        System.IO.StreamReader file = new System.IO.StreamReader(dlg.FileName);
                        lines = new List<string>();
                        String line;
                        while ((line = file.ReadLine()) != null)
                        {
                            lines.Add(line);
                        }
                        submissionFileIndex = 0;

                    }
                    getsubmissions();

                }
            }
        }

        private void getsubmissions()
        {
            XmlNode imagesNode = this.imagesXml.Document.SelectSingleNode("images");

            while (submissionFileIndex < lines.Count)
            {
                this.responseTextBlock.Text = (submissionFileIndex++) + " submission images loaded.";
                NodeFromLineDelegate d = new NodeFromLineDelegate(getNodeFromLine);
                //d.BeginInvoke(line, imagesDir, endGetNodeFromLine, d);
                imagesNode.AppendChild(d.Invoke(lines[submissionFileIndex], imagesDir));

                if (submissionFileIndex % submissionGetLimit == 0)
                    break;
            }
        }

        private const int submissionGetLimit = 100;
        private int submissionFileIndex = 0;
        private List<String> lines;
        private String imagesDir = null;

        delegate XmlNode NodeFromLineDelegate(String line, String imagesDir);

        private delegate void ApendXmlChild(XmlNode node);

        private void endGetNodeFromLine(IAsyncResult result)
        {
            NodeFromLineDelegate d = (NodeFromLineDelegate)result.AsyncState;
            XmlNode imageNode = d.EndInvoke(result);

            ApendXmlChild a = new ApendXmlChild(delegate(XmlNode node)
            {
                XmlNode imagesNode = this.imagesXml.Document.SelectSingleNode("images");
                imagesNode.AppendChild(node);
            });
            this.Dispatcher.BeginInvoke(a, imageNode);

        }

        private XmlNode getNodeFromLine(String line, String imagesDir)
        {

            String[] splited = line.Split(' ');
            String imageName = splited[0];
            XmlNode imageNode = this.imagesXml.Document.CreateNode(XmlNodeType.Element, "image", "");

            XmlNode path = this.imagesXml.Document.CreateNode(XmlNodeType.Element, "path", "");
            path.InnerText = imagesDir + imageName + ".jpg";
            imageNode.AppendChild(path);
            XmlNode pathDisplayNode = this.imagesXml.Document.CreateNode(XmlNodeType.Element, "displaypath", "");
            int k = path.InnerText.LastIndexOf('\\');
            if (k < 0)
                k = path.InnerText.LastIndexOf('/');
            if (k > 0)
                pathDisplayNode.InnerText = path.InnerText.Substring(k + 1);
            else
                pathDisplayNode.InnerText = path.InnerText;
            imageNode.AppendChild(pathDisplayNode);

            XmlNode predictions = this.imagesXml.Document.CreateNode(XmlNodeType.Element, "predictions", "");
            XmlAttribute state = this.imagesXml.Document.CreateAttribute("state", "");
            state.Value = "Loaded from submission file";
            predictions.Attributes.Append(state);

            int i = 1;
            int j = 0;
            while (i < splited.Length)
            {
                XmlNode prediction = this.imagesXml.Document.CreateNode(XmlNodeType.Element, "prediction", "");
                XmlAttribute concept = this.imagesXml.Document.CreateAttribute("concept", "");
                concept.Value = concepts[j++];
                prediction.Attributes.Append(concept);

                XmlNode probability = this.imagesXml.Document.CreateNode(XmlNodeType.Element, "probability", "");
                probability.InnerText = Double.Parse(splited[i++]).ToString("0.0000000000");
                prediction.AppendChild(probability);

                XmlNode binary = this.imagesXml.Document.CreateNode(XmlNodeType.Element, "binary", "");
                binary.InnerText = "" + ((splited[i++].CompareTo("1") == 0) ? true : false);
                prediction.AppendChild(binary);

                predictions.AppendChild(prediction);
            }

            imageNode.AppendChild(predictions);
            return imageNode;
        }


        private void VisualizeButtonButton_Click(object sender, RoutedEventArgs e)
        {
            XmlElement selectedImage = imagesListBox.Items.CurrentItem as XmlElement;
            if (selectedImage == null)
            {
                MessageBox.Show(this, "Please select an image from the Images tab.", "Select image", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            Cursor oldCursor = windowGrid.Cursor;
            windowGrid.Cursor = Cursors.Wait;
            DateTime start = DateTime.Now;
            OnCompletedDelegate o = delegate(String response)
            {
                try
                {
                    byte[] imageBase64Encoding = Convert.FromBase64String(response);
                    BitmapImage bi = new BitmapImage();
                    bi.CacheOption = BitmapCacheOption.OnLoad;
                    bi.BeginInit();
                    bi.StreamSource = new MemoryStream(imageBase64Encoding);
                    bi.EndInit();

                    update("Visualize descriptor in " + (DateTime.Now - start).TotalSeconds.ToString("0.00") + " seconds.");
                    windowGrid.Cursor = oldCursor;

                    new ImageWindow(bi).ShowDialog();
                }
                catch
                {
                    update(response);
                    windowGrid.Cursor = oldCursor;
                }
            };

            Request("visualize " + toBase64(selectedImage.SelectSingleNode("path").InnerText), o);
            update("Visualizing descriptor...");
        }

        private void FaceDetectionButtonButton_Click(object sender, RoutedEventArgs e)
        {
            XmlElement selectedImage = imagesListBox.Items.CurrentItem as XmlElement;
            if (selectedImage == null)
            {
                MessageBox.Show(this, "Please select an image from the Images tab.", "Select image", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            Cursor oldCursor = windowGrid.Cursor;
            windowGrid.Cursor = Cursors.Wait;
            DateTime start = DateTime.Now;
            OnCompletedDelegate o = delegate(String response)
            {
                try
                {
                    byte[] imageBase64Encoding = Convert.FromBase64String(response);
                    BitmapImage bi = new BitmapImage();
                    bi.CacheOption = BitmapCacheOption.OnLoad;
                    bi.BeginInit();
                    bi.StreamSource = new MemoryStream(imageBase64Encoding);
                    bi.EndInit();

                    update("Face detection in " + (DateTime.Now - start).TotalSeconds.ToString("0.00") + " seconds.");
                    windowGrid.Cursor = oldCursor;

                    new ImageWindow(bi).ShowDialog();
                }
                catch
                {
                    update(response);
                    windowGrid.Cursor = oldCursor;
                }
            };

            Request("facedetection " + toBase64(selectedImage.SelectSingleNode("path").InnerText), o);
            update("Face detection...");
        }


        private void ProcessImageButton_Click(object sender, RoutedEventArgs e)
        {
            XmlElement selectedImage = imagesListBox.Items.CurrentItem as XmlElement;
            if (selectedImage == null)
            {
                MessageBox.Show(this, "Please select an image from the Images tab.", "Select image", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            XmlNode predictionsNode = selectedImage.SelectSingleNode("predictions");
            if (predictionsNode == null)
            {
                Cursor oldCursor = windowGrid.Cursor;
                windowGrid.Cursor = Cursors.Wait;
                try
                {
                    DateTime start = DateTime.Now;
                    OnCompletedDelegate o = delegate(String response)
                    {
                        try
                        {
                            XmlDocument doc = new XmlDocument();
                            doc.LoadXml(response);
                            predictionsNode = doc.SelectSingleNode("predictions");
                            XmlNode predictionsCurrNode = this.imagesXml.Document.CreateNode(XmlNodeType.Element, "predictions", "");
                            XmlAttribute dir = this.imagesXml.Document.CreateAttribute("conceptsDir", "");
                            XmlNode concept = predictionsNode.SelectSingleNode("@conceptsDir");
                            dir.Value = concept.Value;

                            XmlAttribute state = this.imagesXml.Document.CreateAttribute("state", "");
                            state.Value = "Processed";

                            predictionsCurrNode.Attributes.Append(dir);
                            predictionsCurrNode.Attributes.Append(state);
                            foreach (XmlNode node in predictionsCurrNode.SelectNodes("probability"))
                            {
                                node.InnerText = Double.Parse(node.InnerText).ToString("0.0000000000");
                            }

                            predictionsCurrNode.InnerXml = predictionsNode.InnerXml;
                            selectedImage.AppendChild(predictionsCurrNode);
                            update("Image processed in " + (DateTime.Now - start).TotalSeconds.ToString("0.00") + " seconds.");
                            
                            windowGrid.Cursor = oldCursor;
                            this.tabControl.SelectedItem = this.predictionTabItem;
                            this.imagesListBox.SelectedItem = selectedImage;
                            this.predictionsListBox.ScrollIntoView(this.predictionsListBox.Items[0]);
                        }
                        catch
                        {
                            update(response);
                            windowGrid.Cursor = oldCursor;
                        }
                    };

                    String imagePath = selectedImage.SelectSingleNode("path").InnerText;
                    String tags = getImageUserTags(imagePath);
                    tags.Replace(" ", "");
                    Request("predict 1 " + tags + " " + toBase64(imagePath), o);
                    update("Processing image...");
                }
                catch
                {
                    MessageBox.Show(this, "Something went wrong while connecting to server.\nPlease be sure the server is online.", "Prediction", MessageBoxButton.OK, MessageBoxImage.Information);
                }
            }
            else
            {
                update("Image already processed.");
                this.tabControl.SelectedItem = this.predictionTabItem;
            }
        }

        private String toBase64(String imagePath)
        {
            return Convert.ToBase64String(File.ReadAllBytes(imagePath));
        }

        private String Request(String command)
        {
            try
            {
                command = command.Replace("\n", "");
                command += "\n";
                State s = new State(serverAdressTextBox.Text);

                UTF8Encoding asen = new UTF8Encoding();
                byte[] ba = asen.GetBytes(command);
                s.netstream.Write(ba, 0, ba.Length);
                s.netstream.Flush();

                using (StreamReader r = new StreamReader(s.netstream))
                {
                    return r.ReadLine();
                }
            }
            catch
            {
                update("Cannot complete request.");
            }
            return null;
        }

        private void Request(String command, OnCompletedDelegate onCompleted)
        {
            try
            {
                command = command.Replace("\n", "");
                command += "\n";
                State s = new State(serverAdressTextBox.Text);
                s.onCompleted += onCompleted;

                UTF8Encoding asen = new UTF8Encoding();
                byte[] ba = asen.GetBytes(command);
                s.netstream.Write(ba, 0, ba.Length);
                s.netstream.Flush();

                s.netstream.BeginRead(s.buffer, 0, s.tcpclnt.ReceiveBufferSize, OnRead, s);
            }
            catch
            {
                update("Cannot complete request.");
            }
        }

        public delegate void OnCompletedDelegate(String response);

        private class State
        {
            public State(String address)
            {
                tcpclnt = new TcpClient();
                tcpclnt.Connect(address, 9998);
                netstream = tcpclnt.GetStream();
                response = new StringBuilder();
                buffer = new byte[tcpclnt.ReceiveBufferSize];
            }

            public byte[] buffer { get; set; }
            public TcpClient tcpclnt { get; set; }
            public NetworkStream netstream { get; set; }
            public StringBuilder response { get; set; }
            public OnCompletedDelegate onCompleted { get; set; }
        }

        private void OnRead(IAsyncResult asyn)
        {
            try
            {
                State s = (State)asyn.AsyncState;
                int nrBytesRecieved = s.netstream.EndRead(asyn);
                if (nrBytesRecieved == 0)
                {
                    s.tcpclnt.Close();
                    s.netstream.Close();
                    s.netstream.Dispose();
                    //s.response.ToString()
                    this.Dispatcher.BeginInvoke(DispatcherPriority.Normal, s.onCompleted, s.response.ToString());
                }
                else
                {
                    s.response.Append(Encoding.UTF8.GetString(s.buffer, 0, nrBytesRecieved));
                    s.netstream.BeginRead(s.buffer, 0, s.tcpclnt.ReceiveBufferSize, OnRead, s);
                }
            }
            catch
            {
                update("Connection lost.");
            }
        }

        private void PostAnnotationsButton_Click(object sender, RoutedEventArgs e)
        {
            XmlElement selectedImage = imagesListBox.Items.CurrentItem as XmlElement;
            if (selectedImage == null)
            {
                MessageBox.Show(this, "Please select an image from the Images tab.", "Select image", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            XmlNode predictionsNode = selectedImage.SelectSingleNode("predictions");
            if (predictionsNode == null)
            {
                MessageBox.Show(this, "Please process the image first.", "Process image", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            OnCompletedDelegate o = delegate(String response)
            {
                update(response);
            };

            XmlNode state = selectedImage.SelectSingleNode("predictions/@state");
            state.Value = "Posted to server";
            Request("post " + "<image>" + selectedImage.InnerXml + "</image>", o);

        }

        private void textBox_TextChanged(object sender, TextChangedEventArgs e)
        {
            TextBox textBox = sender as TextBox;
            double iValue = -1;
            if (Double.TryParse(textBox.Text, out iValue) == false)
            {
                foreach (var change in e.Changes)
                {
                    int iAddedLength = change.AddedLength;
                    int iOffset = change.Offset;
                    textBox.Text = textBox.Text.Remove(iOffset, iAddedLength);
                }
            }

            XmlElement selectedImage = imagesListBox.Items.CurrentItem as XmlElement;
            if (selectedImage != null)
            {
                XmlNode state = selectedImage.SelectSingleNode("predictions/@state");
                state.Value = "Manually annotated";
            }
        }
        private void OnKeyDownHandler(object sender, KeyEventArgs e)
        {
            if (e.Key == Key.Return)
            {
                this.predictionsListBox.Focus();

                ListCollectionView lcv = (ListCollectionView)(CollectionViewSource.GetDefaultView(this.predictionsListBox.ItemsSource));
                lcv.Refresh();
            }
        }
    }
}
