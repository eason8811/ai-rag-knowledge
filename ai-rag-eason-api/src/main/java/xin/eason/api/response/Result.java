package xin.eason.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标准结果类, 用于返回 API 接口响应
 *
 * @param <T> 结果 data 的泛型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    /**
     * 结果代码
     */
    private Integer code;
    /**
     * 结果信息
     */
    private String info;
    /**
     * 实际结果数据
     */
    private T data;

    /**
     * 返回成功的结果, 可以指定返回代码和具体数据
     *
     * @param code 返回代码
     * @param data 具体数据
     * @param <T>  具体数据的泛型
     * @return {@link Result} 标准结果对象
     */
    public static <T> Result<T> success(Integer code, T data) {
        Result<T> result = new Result<>();
        result.data = data;
        result.code = code;
        result.info = "success";
        return result;
    }

    /**
     * {@link Result#success(Integer, Object)} 方法的重载, 返回代码默认为 1
     *
     * @param data 具体数据
     * @param <T>  具体数据的泛型
     * @return {@link Result} 标准结果对象
     */
    public static <T> Result<T> success(T data) {
        return success(1, data);
    }

    /**
     * 返回出现错误时的标准返回结果
     *
     * @param info 结果信息
     * @param <T>  具体数据的泛型
     * @return {@link Result} 标准结果对象
     */
    public static <T> Result<T> error(String info) {
        Result<T> result = new Result<>();
        result.info = info;
        result.code = 0;
        result.data = null;
        return result;
    }

}
