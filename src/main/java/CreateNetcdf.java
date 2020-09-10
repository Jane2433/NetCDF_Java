import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @CreateTime: 2020/9/10 11:32
 * @Author: zzl
 * @Description: 创建 nc 文件
 */
public class CreateNetcdf {

    public static void create(String ncPath) {
        NetcdfFileWriteable newNcFile = null;
        try {
            newNcFile = NetcdfFileWriteable.createNew(ncPath.substring(0, ncPath.length() - 3) + "_new.nc");

            Dimension lonDim = newNcFile.addDimension("lon", 240);
            Dimension latDim = newNcFile.addDimension("lat", 120);

            List<Dimension> lonDimensions = Collections.singletonList(lonDim);
            newNcFile.addVariable("lon", DataType.DOUBLE, lonDimensions);
            newNcFile.addVariableAttribute("lon", "standard_name", "longitude");
            newNcFile.addVariableAttribute("lon", "units", "degrees_east");

            newNcFile.addVariable("lat", DataType.DOUBLE, Collections.singletonList(latDim));
            newNcFile.addVariableAttribute("lat", "standard_name", "latitude");
            newNcFile.addVariableAttribute("lat", "units", "degrees_north");

            newNcFile.addVariable("tmin", DataType.FLOAT, Arrays.asList(latDim, lonDim));
            newNcFile.addVariableAttribute("tmin", "coordinates", "lon lat");
            newNcFile.addVariableAttribute("tmin", "units", "Degree");
            newNcFile.addVariableAttribute("tmin", "missing_value", -3.402823E38f);

            //Array tminValue = Array.factory(int.class, new int[] { 4 }, new int[] { 6, 4, 99, 103 });
            //newNcFile.addVariableAttribute("tmin", "missing_value", tminValue);

            newNcFile.create();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (newNcFile != null) {
                    newNcFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        create("D:\\temperature.nc");
    }

}
