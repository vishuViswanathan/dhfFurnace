package materials;

/**
 * Created by viswanathanm on 04-09-2017.
 */
public class EditFuelData extends FuelData{

    public EditFuelData() {
       super();
       appCode = 109;
       bCanEdit = true;
    }

    public static void main(String[] args)  {
        EditFuelData fD = new EditFuelData();
        fD.setItUp();
    }
}
