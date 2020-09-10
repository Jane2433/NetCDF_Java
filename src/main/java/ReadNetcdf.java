import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;
import java.util.List;

/**
 * @CreateTime: 2020/9/9 20:38
 * @Author: zzl
 * @Description: 读取 nc 文件
 */
public class ReadNetcdf {

    public static void read(String ncPath) {

        NetcdfFile ncFile = null;
        try {
            // 打开 nc 文件
            ncFile = NetcdfDataset.open(ncPath);

            // 读取维度信息
            List<Dimension> dimensions = ncFile.getDimensions();
            Dimension lonDim = dimensions.get(0);
            Dimension latDim = dimensions.get(1);

            Dimension yearDim = ncFile.findDimension("year");

            // 全局属性
            List<Attribute> globalAttributes = ncFile.getGlobalAttributes();
            Attribute gAttribute1 = globalAttributes.get(0);

            Attribute gAttribute2 = ncFile.findGlobalAttribute("Source_Software");

            // 读取变量信息
            List<Variable> variables = ncFile.getVariables();
            Variable lonVar = variables.get(0);
            Variable latVar = variables.get(1);
            Variable yearVar = variables.get(2);

            Variable tminVar = ncFile.findVariable("tmin");

            // 读取变量属性
            List<Attribute> tminAttributes = tminVar.getAttributes();
            Attribute tminAttribute2 = tminVar.findAttribute("units");

            // 读取变量维度
            List<Dimension> tminDimensions = tminVar.getDimensions();
            int yearIndex = tminVar.findDimensionIndex("year");
            Dimension tminYearDimension = tminDimensions.get(yearIndex);

            // 读取一维数据
            double[] lat = (double[]) latVar.read().copyTo1DJavaArray();
            // 读取多维数据
            float[][][] tmin = (float[][][]) tminVar.read().copyToNDJavaArray();

            int[] origin = new int[] { 0, 0, 0 };
            int[] size = new int[] { yearDim.getLength(), latDim.getLength(), lonDim.getLength() };

            float[][][] tminRange = (float[][][]) tminVar.read(origin, size).copyToNDJavaArray();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (ncFile != null) {
                    ncFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        read("D:\\temperature.nc");
    }

}
