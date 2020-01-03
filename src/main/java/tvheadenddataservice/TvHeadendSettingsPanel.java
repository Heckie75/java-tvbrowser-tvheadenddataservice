package tvheadenddataservice;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import tvdataservice.SettingsPanel;

public class TvHeadendSettingsPanel extends SettingsPanel {

  private static final long serialVersionUID = 1L;

  private Properties settings;

  private JTextField cUrl;
  private JTextField cUser;
  private JPasswordField cPassword;

  public TvHeadendSettingsPanel(Properties settings) {

    this.settings = settings;
    createGui();
  }

  private static GridBagConstraints createGridBagConstraint(int x, int y, int width, int height) {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = x;
    gbc.gridy = y;
    gbc.gridwidth = width;
    gbc.gridheight = height;
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.LINE_START;
    gbc.weightx = 100;
    gbc.weighty = 100;
    return gbc;
  }

  private void createGui() {

    // layout
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc;
    setLayout(gbl);

    JComponent comp;

    gbc = createGridBagConstraint(0, 0, 1, 1);
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 1;
    gbc.weighty = 0;
    comp = new JLabel("URL:");
    gbl.setConstraints(comp, gbc);
    add(comp);

    gbc = createGridBagConstraint(1, 0, 4, 1);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 500;
    String url = Optional.ofNullable(settings.getProperty(TvHeadendDataService.TVHEADENDDATASERVICE_URL))
        .orElse(TvHeadendDataService.DEFAULT_URL);
    cUrl = new JTextField(url);
    gbl.setConstraints(cUrl, gbc);
    add(cUrl);

    gbc = createGridBagConstraint(0, 1, 1, 1);
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 1;
    comp = new JLabel("Username:");
    gbl.setConstraints(comp, gbc);
    add(comp);

    gbc = createGridBagConstraint(1, 1, 4, 1);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 500;
    cUser = new JTextField(settings.getProperty(TvHeadendDataService.TVHEADENDDATASERVICE_USER));
    gbl.setConstraints(cUser, gbc);
    add(cUser);

    gbc = createGridBagConstraint(0, 2, 1, 1);
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 1;
    comp = new JLabel("Password:");
    gbl.setConstraints(comp, gbc);
    add(comp);

    gbc = createGridBagConstraint(1, 2, 4, 1);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 500;
    cPassword = new JPasswordField(settings.getProperty(TvHeadendDataService.TVHEADENDDATASERVICE_PASSWORD));
    gbl.setConstraints(cPassword, gbc);
    add(cPassword);

  }

  @Override
  public void ok() {
    String url = cUrl.getText().trim();
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      url = "http://" + url;
    }
    if (!url.endsWith("/")) {
      url += "/";
    }
    settings.put(TvHeadendDataService.TVHEADENDDATASERVICE_URL, url);
    cUrl.setText(url);

    String user = cUser.getText().trim();
    settings.put(TvHeadendDataService.TVHEADENDDATASERVICE_USER, user);
    cUser.setText(user);

    String password = new String(cPassword.getPassword()).trim();
    settings.put(TvHeadendDataService.TVHEADENDDATASERVICE_PASSWORD, password);
    cPassword.setText(password);
  }

}
