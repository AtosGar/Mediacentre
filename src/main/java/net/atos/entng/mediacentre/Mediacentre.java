package net.atos.entng.mediacentre;

import net.atos.entng.mediacentre.controllers.MediacentreController;
import org.entcore.common.http.BaseServer;
import org.entcore.common.http.filter.ShareAndOwner;


public class Mediacentre extends BaseServer {

    public final static String MEDIACENTRE_COLLECTION = "mediacentre";

    @Override
    public void start() {
        super.start();
        addController(new MediacentreController(MEDIACENTRE_COLLECTION));
        setDefaultResourceFilter(new ShareAndOwner());
    }
}
