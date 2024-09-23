package business.order;

import api.ApiException;
import business.BookstoreDbException;
import business.book.Book;
import business.book.BookDao;
import business.cart.ShoppingCart;
import business.cart.ShoppingCartItem;
import business.customer.Customer;
import business.customer.CustomerForm;
import business.order.OrderDao;
import business.order.LineItemDao;
import business.customer.CustomerDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import business.JdbcUtils;

public class DefaultOrderService implements OrderService {

	private BookDao bookDao;
	private OrderDao orderDao;
	private LineItemDao lineItemDao;
	private CustomerDao customerDao;

	public void setBookDao(BookDao bookDao) {
		this.bookDao = bookDao;
	}
	public void setOrderDao(OrderDao orderDao) { this.orderDao = orderDao; }
	public void setLineItemDao(LineItemDao lineItemDao) { this.lineItemDao = lineItemDao; }
	public void setCustomerDao(CustomerDao customerDao) { this.customerDao = customerDao; }

	@Override
	public OrderDetails getOrderDetails(long orderId) {
		Order order = orderDao.findByOrderId(orderId);
		Customer customer = customerDao.findByCustomerId(order.customerId());
		//List<LineItem> lineItems = lineItemDao.findByOrderId(customer.customerId());
		List<LineItem> lineItems = lineItemDao.findByOrderId(orderId);

		List<Book> books = lineItems
				.stream()
				.map(lineItem -> bookDao.findByBookId(lineItem.bookId()))
				.toList();
		return new OrderDetails(order, customer, lineItems, books);
	}

	@Override
    public long placeOrder(CustomerForm customerForm, ShoppingCart cart) {

		validateCustomer(customerForm);
		validateCart(cart);

		try (Connection connection = JdbcUtils.getConnection()) {
			Date ccExpDate = getCardExpirationDate(
					customerForm.getCcExpiryMonth(),
					customerForm.getCcExpiryYear());
			return performPlaceOrderTransaction(
					customerForm.getName(),
					customerForm.getAddress(),
					customerForm.getPhone(),
					customerForm.getEmail(),
					customerForm.getCcNumber(),
					ccExpDate, cart, connection);
		} catch (SQLException e) {
			throw new BookstoreDbException("Error during close connection for customer order", e);
		}

	}

	private Date getCardExpirationDate(String monthString, String yearString) {
		//Date currentDate = new Date();
		//Date date = new Date(Integer.parseInt(yearString), Integer.parseInt(monthString), currentDate.getDate() )
		//return new Date(); // TODO Implement this correctly

		// Parse the month and year strings into their respective integer values
		int month = Integer.parseInt(monthString);
		int year = Integer.parseInt(yearString);

		// Create a Calendar object and set the month and year
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.MONTH, month - 1); // Months are 0-based in Calendar
		calendar.set(Calendar.YEAR, year);

		// Set the day of the month to the last day of the month
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));

		// Return the expiration date as a Date object
		return calendar.getTime();
	}

	private long performPlaceOrderTransaction(
			String name, String address, String phone,
			String email, String ccNumber, Date date,
			ShoppingCart cart, Connection connection) {
		try {
			connection.setAutoCommit(false);
			long customerId = customerDao.create(
					connection, name, address, phone, email,
					ccNumber, date);
			long customerOrderId = orderDao.create(
					connection,
					cart.getComputedSubtotal() + cart.getSurcharge(),
					generateConfirmationNumber(), customerId);
			for (ShoppingCartItem item : cart.getItems()) {
				lineItemDao.create(connection, item.getBookId(),
						customerOrderId, item.getQuantity());
			}
			connection.commit();
			return customerOrderId;
		} catch (Exception e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				throw new BookstoreDbException("Failed to roll back transaction", e1);
			}
			return 0;
		}
	}

	private int generateConfirmationNumber() {
		return ThreadLocalRandom.current().nextInt(999999999);
	}

	private void validateCustomer(CustomerForm customerForm) {

    	String name = customerForm.getName();
		String address = customerForm.getAddress();
		String phone = customerForm.getPhone();
		String email = customerForm.getEmail();
		String ccNumber = customerForm.getCcNumber();
		String ccExpiryMonth = 	customerForm.getCcExpiryMonth();
		String ccExpiryYear = customerForm.getCcExpiryYear();

		if (name == null || name.equals("") || name.length() > 45 || name.length() < 4) {
			//throw new ApiException.InvalidParameter("Invalid name field");
			throw new ApiException.ValidationFailure(name, "Invalid name field");
		}

		if (address == null || address.equals("") || address.length() > 45 || address.length() < 4) {
			//throw new ApiException.InvalidParameter("Invalid name field");
			throw new ApiException.ValidationFailure(address, "Invalid address field");
		}

		if (phone == null || phone.equals("")) {
			//throw new ApiException.InvalidParameter("Invalid name field");
			throw new ApiException.ValidationFailure(phone, "Invalid phone number field");
		}
		String normalPhone = phone.replace("[^0-9]", "");
		if (normalPhone.length() != 10) {
			throw new ApiException.ValidationFailure(normalPhone, "Invalid phone number field");
		}

		if (email == null || email.equals("") || email.contains(" ") || !email.contains("@") || email.endsWith(".")) {
			//throw new ApiException.InvalidParameter("Invalid name field");
			throw new ApiException.ValidationFailure(email, "Invalid email field");
		}

		if (ccNumber == null || ccNumber.equals("")) {
			//throw new ApiException.InvalidParameter("Invalid name field");
			throw new ApiException.ValidationFailure(ccNumber, "Invalid card number field");
		}
		String normalCcNumber = ccNumber.replace("[^0-9]", "");
		if (normalCcNumber.length() < 14 || normalCcNumber.length() > 16) {
			throw new ApiException.ValidationFailure(normalCcNumber, "Invalid card number field");
		}

		if (expiryDateIsInvalid(customerForm.getCcExpiryMonth(), customerForm.getCcExpiryYear())) {
			//throw new ApiException.InvalidParameter("Invalid expiry date");
			throw new ApiException.ValidationFailure("Please enter a valid expiration date.");
		}
	}

	private boolean expiryDateIsInvalid(String ccExpiryMonth, String ccExpiryYear) {

		// TODO: return true when the provided month/year is before the current month/yeaR
		// HINT: Use Integer.parseInt and the YearMonth class
		// Get the current year and month
		YearMonth currentYearMonth = YearMonth.now();
		// Extract the year and month
		int currentYear = currentYearMonth.getYear();
		int currentMonth = currentYearMonth.getMonthValue();

        return Integer.parseInt(ccExpiryYear) < currentYear || Integer.parseInt(ccExpiryMonth) < currentMonth;

    }

	private void validateCart(ShoppingCart cart) {

		if (cart.getItems().size() <= 0) {
			//throw new ApiException.InvalidParameter("Cart is empty.");
			throw new ApiException.ValidationFailure("Cart is empty.");
		}

		cart.getItems().forEach(item-> {
			if (item.getQuantity() < 0 || item.getQuantity() > 99) {
				//throw new ApiException.InvalidParameter("Invalid quantity");
				throw new ApiException.ValidationFailure("Invalid quantity");
			}
			Book databaseBook = bookDao.findByBookId(item.getBookId());
			// TODO: complete the required validations
			if (item.getBookForm().getPrice() != databaseBook.price()) {
				throw new ApiException.ValidationFailure("Invalid price");
			}
			if (item.getBookForm().getCategoryId() != databaseBook.categoryId()) {
				throw new ApiException.ValidationFailure("Invalid category");
			}
		});
	}

}
