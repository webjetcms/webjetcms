package sk.iway.iwcm.system.audit;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.i18n.Prop;

@Service
public class AuditService {

	public List<LabelValueDetails> getTypes(HttpServletRequest request) {
		List<LabelValueDetails> types = new ArrayList<>();
		Prop prop = Prop.getInstance(request);

		for (Integer type : Adminlog.getTypes()) {
			LabelValueDetails lvd = new LabelValueDetails();
			lvd.setValue(type.toString());
			lvd.setLabel(prop.getText("components.adminlog." + type));
			types.add(lvd);
		}

		return types;
	}

}
