package materials;

/**
 * Created by viswanathanm on 04-09-2017.
 */
public class EditThermalProperties extends ThermalProperties{

    public EditThermalProperties() {
        super();
        appCode = 107;
        bCanEdit = true;
    }

    public static void main(String[] args)  {
        EditThermalProperties tP= new EditThermalProperties();
        tP.setItUp();
    }
}
