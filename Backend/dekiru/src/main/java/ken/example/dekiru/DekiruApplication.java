package ken.example.dekiru;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
	@EnableScheduling
public class DekiruApplication {

	public static void main(String[] args) {
		SpringApplication.run(DekiruApplication.class, args);
	}
/*
 Stream trong Java
Stream: là chuỗi các phần tử từ collection, array, hoặc nguồn dữ liệu khác, dùng để xử lý dữ liệu mà không thay đổi dữ liệu gốc.
Lambda hoặc method reference thường được dùng để xử lý từng phần tử trong Stream.
Các operation phổ biến

filter

Dùng để lọc phần tử theo điều kiện.
Lambda phải trả về boolean.
Ví dụ:
List<Integer> nums = List.of(1,2,3,4);
List<Integer> even = nums.stream()
                         .filter(n -> n % 2 == 0)
                         .toList(); // [2,4]

map

Dùng để chuyển đổi từng phần tử sang giá trị khác.
Lambda nhận phần tử của Stream và trả về giá trị mới → kiểu dữ liệu của Stream sau map = kiểu giá trị trả về của lambda.
Ví dụ:
List<String> names = List.of("Alice", "Bob");
List<Integer> lengths = names.stream()
                             .map(n -> n.length())
                             .toList(); // [5,3]
toList() / collect(Collectors.toList())
Dùng để xuất kết quả của Stream thành List, hoặc các collection khác.
Stream sau khi xử lý vẫn là Stream, phải dùng terminal operation này để lấy dữ liệu ra.


Lambda trong Java
Lambda là cách viết gọn của một method, dùng để implement method trừu tượng trong functional interface.
Điều kiện: interface đó chỉ có duy nhất một method trừu tượng (functional interface).
1. Cấu trúc chung
(parameters) -> expression
parameters = tham số truyền vào lambda
expression = logic xử lý, trả về giá trị (nếu interface yêu cầu)
2. Các dạng lambda
Có tham số
n -> n % 2 == 0  // n là tham số, trả về boolean
Dùng khi method abstract có 1 tham số (Predicate<T>).
Nếu nhiều tham số:
(a,b) -> a + b   // dùng với BiFunction<T,U,R>
Không có tham số
() -> System.out.println("Hello")
Dùng khi method abstract không có tham số (Runnable, Supplier<T>).
Nhiều dòng
n -> {
    int square = n * n;
    return square % 2 == 0;
}
Nếu functional interface yêu cầu trả về giá trị, phải dùng return.
Nếu interface trả về void, không cần return.

*/


}
