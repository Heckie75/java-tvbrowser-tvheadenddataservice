package tvheadenddataservice;

import java.awt.BorderLayout;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import devplugin.Channel;
import devplugin.ChannelFilter;
import tvbrowser.core.Settings;
import util.ui.ChannelListCellRenderer;

public class ChannelChooserPanel extends JPanel {

  private DefaultListModel<Object> mChannelChooserModel;
  private JList<Object> mList;
  private boolean disableSync = false;
  private ChannelFilter mChannelFilter;

  /**
   * @param frame
   *          The mainframe.
   * @param keyListener
   *          The key listener for FAYT.
   */
  public ChannelChooserPanel() {
    mChannelChooserModel = new DefaultListModel<>();

    mList = new JList<>(mChannelChooserModel) {
      @Override
      public void setSelectedIndex(int index) {
        if (index >= 0 && getModel().getElementAt(index) instanceof String) {
          int test = getSelectedIndex();

          if (test < index) {
            index++;
          } else {
            index--;
          }

          if (index >= 0 && index < getModel().getSize()) {
            setSelectedIndex(index);
          }
        } else {
          super.setSelectedIndex(index);
        }
      }
    };
    updateChannelChooser();
    setLayout(new BorderLayout());
    JScrollPane scrollPane = new JScrollPane(mList);
    add(scrollPane);
  }

  public void updateChannelChooser() {
    mList.setCellRenderer(new ChannelListCellRenderer(Settings.propShowChannelIconsInChannellist.getBoolean(),
        Settings.propShowChannelNamesInChannellist.getBoolean(), false, false, true, true));
    mChannelChooserModel.removeAllElements();
    Channel[] channelList = tvbrowser.core.ChannelList.getSubscribedChannels();

    String[] separatorArr = Settings.propSubscribedChannelsSeparators.getStringArray();
    Channel previousChannel = null;
    int lastSeparatorIndex = 0;

    if (channelList.length > 0) {
      mChannelChooserModel.addElement(channelList[0]);
      previousChannel = channelList[0];
    }

    for (int i = 1; i < channelList.length; i++) {
      for (int j = lastSeparatorIndex; j < separatorArr.length; j++) {
        String separator = separatorArr[j];

        if (separator.endsWith(channelList[i].getUniqueId()) &&
            previousChannel != null && separator.startsWith(previousChannel.getUniqueId())) {
          mChannelChooserModel.addElement(Channel.SEPARATOR);
          lastSeparatorIndex = j + 1;
        }
      }

      previousChannel = channelList[i];

      if (channelList[i - 1].getJointChannel() == null ||
          !channelList[i - 1].getJointChannel().equals(channelList[i])) {
        mChannelChooserModel.addElement(channelList[i]);
      }
    }
  }

}
