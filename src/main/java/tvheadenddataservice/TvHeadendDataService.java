package tvheadenddataservice;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.Icon;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.MalformedChallengeException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.heckie.tvheadend.api.TvheadendHttpClient;
import de.heckie.tvheadend.api.model.KeyValList;
import de.heckie.tvheadend.api.model.channel.ChannelGrid;
import de.heckie.tvheadend.api.model.epg.Event;
import devplugin.AbstractTvDataService;
import devplugin.Channel;
import devplugin.ChannelGroup;
import devplugin.ChannelGroupImpl;
import devplugin.Date;
import devplugin.PluginInfo;
import devplugin.Program;
import devplugin.ProgramFieldType;
import devplugin.ProgressMonitor;
import devplugin.Version;
import tvdataservice.MutableChannelDayProgram;
import tvdataservice.MutableProgram;
import tvdataservice.SettingsPanel;
import tvdataservice.TvDataUpdateManager;
import util.exc.TvBrowserException;
import util.ui.ImageUtilities;

public class TvHeadendDataService extends AbstractTvDataService {

  private final static Logger logger = Logger.getLogger(TvHeadendDataService.class.getName());

  private static final String TV_HEADEND_DATASERVICE = "TVHeadend Dataservice";
  private static final Version VERSION = new Version(0, 1, 0);

  static final String DEFAULT_URL = "http://localhost:9981/";

  static final String TVHEADENDDATASERVICE_PASSWORD = "tvheadenddataservice.password";
  static final String TVHEADENDDATASERVICE_USER = "tvheadenddataservice.user";
  static final String TVHEADENDDATASERVICE_URL = "tvheadenddataservice.url";
  static final String TVHEADENDDATASERVICE_CHANNELS = "tvheadenddataservice.channels";

  private final static SimpleDateFormat SDF = new SimpleDateFormat("YYYY-MM-DD");

  private final static List<ChannelGroup> channelGroups = List
      .of(new ChannelGroupImpl(TV_HEADEND_DATASERVICE, TV_HEADEND_DATASERVICE, TV_HEADEND_DATASERVICE));

  private File dataDir;
  private ChannelGrid channelGrid;

  private KeyValList genreTypes;

  private Properties settings = new Properties();

  public static Version getVersion() {
    return VERSION;
  }

  public TvHeadendDataService() {

  }

  private TvheadendHttpClient getClient() {
    try {
      String url = Optional.ofNullable(settings.getProperty(TVHEADENDDATASERVICE_URL)).orElse("");
      String user = Optional.ofNullable(settings.getProperty(TVHEADENDDATASERVICE_USER)).orElse("");
      String password = Optional.ofNullable(settings.getProperty(TVHEADENDDATASERVICE_PASSWORD)).orElse("");
      return new TvheadendHttpClient(url, user, password);
    } catch (MalformedChallengeException | IOException e) {
      logger.log(Level.SEVERE,
          String.format("Unable to initiate tvheadend client", e.getMessage()));
    }
    return null;
  }

  @Override
  public void setWorkingDirectory(File dataDir) {
    this.dataDir = dataDir;
  }

  @Override
  public void loadSettings(Properties settings) {
    this.settings = settings;

    try {
      String channelString = settings.getProperty(TVHEADENDDATASERVICE_CHANNELS);
      if (!StringUtils.isBlank(channelString)) {
        channelGrid = new ObjectMapper().readValue(channelString,
            ChannelGrid.class);
      }
    } catch (IOException e) {
    }
  }

  @Override
  public Properties storeSettings() {
    return settings;
  }

  @Override
  public boolean hasSettingsPanel() {
    return true;
  }

  @Override
  public SettingsPanel getSettingsPanel() {
    return new TvHeadendSettingsPanel(settings);
  }

  @Override
  public boolean supportsDynamicChannelList() {
    return true;
  }

  @Override
  public boolean supportsDynamicChannelGroups() {
    return false;
  }

  @Override
  public ChannelGroup[] getAvailableGroups() {
    return channelGroups.toArray(new ChannelGroup[channelGroups.size()]);
  }

  @Override
  public Channel[] getAvailableChannels(ChannelGroup group) {
    return convertFromChannelGrid();
  }

  @Override
  public ChannelGroup[] checkForAvailableChannelGroups(ProgressMonitor monitor) throws TvBrowserException {
    return getAvailableGroups();
  }

  private Channel[] convertFromChannelGrid() {

    if (channelGrid == null) {
      return null;
    }

    List<Channel> channels = channelGrid.getEntries().stream().map(c -> {
      Icon icon = ImageUtilities.createImageIconFromJar("tvheadend_small.png", getClass());
      Channel channel = new Channel(TvHeadendDataService.this,
          c.getName(),
          c.getUuid(),
          TimeZone.getDefault(),
          Locale.getDefault().getLanguage(),
          "",
          "",
          channelGroups.get(0),
          icon,
          Channel.CATEGORY_DIGITAL,
          c.getName());
      return channel;
    }).collect(Collectors.toList());
    return channels.toArray(new Channel[channels.size()]);
  }

  @Override
  public Channel[] checkForAvailableChannels(ChannelGroup group, ProgressMonitor monitor) throws TvBrowserException {
    TvheadendHttpClient client = getClient();
    try {
      channelGrid = client.getChannelGrid(true);
      settings.put(TVHEADENDDATASERVICE_CHANNELS, new ObjectMapper().writeValueAsString(channelGrid));
    } catch (IOException e) {
      throw new TvBrowserException(TvHeadendDataService.class, "", "Cannot get channels", e);
    }
    return convertFromChannelGrid();
  }

  @Override
  public void updateTvData(TvDataUpdateManager updateManager, Channel[] channelArr, Date startDate, int dateCount,
      ProgressMonitor monitor) throws TvBrowserException {

    List<String> channelUuids = List.of(channelArr).stream().map(c -> c.getId()).collect(Collectors.toList());
    TvheadendHttpClient client = getClient();

    java.util.Date startRealDate = new java.util.Date(startDate.getYear() - 1900, startDate.getMonth() - 1,
        startDate.getDayOfMonth());
    long time = startRealDate.getTime();
    try {
      List<Event> eventsInPeriod = client.getEvents(true, channelUuids, time,
          time + dateCount * 86400000L);

      for (int c = 0; c < channelArr.length; c++) {
        for (int d = 0; d < dateCount; d++) {

          long day = time + d * 86400000L;

          Date tvDate = Date.createYYYYMMDD(SDF.format(new java.util.Date(day)), "-");

          Channel channel = channelArr[c];
          MutableChannelDayProgram channelDayProgram = new MutableChannelDayProgram(tvDate,
              channel);

          eventsInPeriod.stream()
              .filter(e -> e.getChannelUuid().equals(channel.getId()))
              .filter(e -> e.getStart() * 1000 >= day && e.getStart() * 1000 < (day + 86400000L))
              .map(e -> {
                MutableProgram program = new MutableProgram(channel, tvDate,
                    e.getStartDate().getHours(),
                    e.getStartDate().getMinutes(), true);
                String custom = "tvheadend.eventid=" + String.valueOf(e.getEventId());
                program.setTextField(ProgramFieldType.CUSTOM_TYPE, custom);
                program.setTitle(e.getTitle());
                String shortInfo = "";
                shortInfo += e.getSubtitle() != null ? e.getSubtitle() + "\n" : "";
                if (e.getDescription() != null) {
                  shortInfo += MutableProgram.generateShortInfoFromDescription(e.getDescription());
                  program.setDescription(e.getDescription());
                }
                program.setShortInfo(shortInfo);
                program.setInfo(parseInfo(e));

                program.setLength((int) ((e.getStop() - e.getStart()) / 60L));
                program.setProgramLoadingIsComplete();
                return program;
              }).forEach(p -> channelDayProgram.addProgram(p));

          channelDayProgram.setWasChangedByPlugin();
          updateManager.updateDayProgram(channelDayProgram);
        }
      }
    } catch (IOException e) {
      throw new TvBrowserException(TvHeadendDataService.class, "", "Cannot get channels", e);
    }

  }

  private int parseInfo(Event e) {
    int info = 0;
    info |= e.getHd() != 0 ? Program.INFO_VISION_HD : 0;
    info |= e.getBw() != 0 ? Program.INFO_VISION_BLACK_AND_WHITE : 0;
    info |= e.getSubtitled() != 0 ? Program.INFO_ORIGINAL_WITH_SUBTITLE : 0;
    info |= e.getWidescreen() != 0 ? Program.INFO_VISION_16_TO_9 : 0;

    int[] genres = e.getGenre();
    if (genres != null) {
      for (int g = 0; g < genres.length; g++) {
        for (String s : getGenretypesById(genres[g])) {
          switch (s) {
          case "Arts / Culture (without music)":
          case "Music / Ballet / Dance":
            info |= Program.INFO_CATEGORIE_ARTS;
            break;
          case "Movie / Drama":
            info |= Program.INFO_CATEGORIE_MOVIE;
            break;
          case "Children's / Youth programs":
            info |= Program.INFO_CATEGORIE_CHILDRENS;
            break;
          case "Education / Science / Factual topics":
            info |= Program.INFO_CATEGORIE_DOCUMENTARY;
            break;
          case "Leisure hobbies":
            info |= Program.INFO_CATEGORIE_MAGAZINE_INFOTAINMENT;
            break;
          case "News / Current affairs":
          case "Social / Political issues / Economics":
            info |= Program.INFO_CATEGORIE_NEWS;
            break;
          case "Show / Game show":
            info |= Program.INFO_CATEGORIE_SHOW;
            break;
          case "Sports":
            info |= Program.INFO_CATEGORIE_SPORTS;
            break;
          }
        }
      }
    }
    return info;
  }

  private List<String> getGenretypesById(int genre) {
    if (genreTypes == null) {
      try {
        genreTypes = getClient().getGenreTypes(false);
      } catch (IOException e) {
      }
    }

    return genreTypes.getEntries().stream().filter(kv -> {
      int intValue = Integer.valueOf(kv.getKey()).intValue();
      return (intValue & genre) == intValue;
    }).map(kv -> kv.getVal()).collect(Collectors.toList());
  }

  @Override
  public PluginInfo getInfo() {
    PluginInfo pluginInfo = new PluginInfo(this.getClass(),
        TV_HEADEND_DATASERVICE,
        "A data service for TV-Browser which provides EPG data loaded from your TvHeadend Server",
        "heckie75",
        "MIT",
        "https://github.com/Heckie75/java-tvbrowser-tvheadenddataservice");
    return pluginInfo;
  }

}