import { asDollarsAndCents } from "../../services/utils";
import { useOrderDetails } from "../../contexts/OrderDetailsContext";
import { useCart } from "../../contexts/CartContext";
import "./ConfirmationPage.css";
import { BookItem } from "../../types/types";

export default function ConfirmationPage() {
  const { orderDetails } = useOrderDetails();
  console.log("conf page order details", orderDetails);

  const { cart, dispatch } = useCart();
  /*const getQuantity = (book: BookItem) => {
    const item = cart.items.find((item) => item.book.title === book.title);
    return item?.quantity;
  };*/
  dispatch({ type: 'CLEAR_CART' });
  const getQuantity = (book: BookItem) => {
    const item = orderDetails.lineItems.find((item) => item.bookId  === book.bookId );
    return item?.quantity;
  };

  const customerDate = new Date(orderDetails.customer.ccExpDate);

  return (
    <div id="confirmation">
      <section id="confirmation-summary">
        <h1 style={{marginBottom:"1em"}}>Confirmation</h1>
        <p style={{textAlign:"left"}}>
          Your confirmation number is {orderDetails.order.confirmationNumber}
        </p>
        <p style={{textAlign:"left"}}>{new Date(orderDetails.order.dateCreated).toString()}</p>
      </section>
      <section id="confirmation-order-details">
        <table id="order-details-table">
          <thead>
            <tr>
              <th>Product</th>
              <th>Quantity</th>
              <th>Price</th>
            </tr>
          </thead>
          <tbody>
            {orderDetails.books.map((book) => (
              <tr key={book.title}>
                <td className = "confirmation-book-title">{book.title}</td>
                <td className = "confirmation-book-quantity">{getQuantity(book)}</td>
                <td className = "confirmation-book-amount">
                  {asDollarsAndCents(book.price * (getQuantity(book) || 1)*100)}
                </td>
              </tr>
            ))}

            <tr className = "confirmation-delivery-surcharge">
              <td colSpan={2}>-- Delivery Surcharge --</td>
              <td className = "confirmation-cart-surcharge">{asDollarsAndCents(cart.surcharge)}</td>
            </tr>

            <tr>
              <td colSpan={2}>
                <strong>Total</strong>
              </td>
              <td className = "confirmation-cart-total">
                <strong>{asDollarsAndCents(orderDetails.order.amount*100)}</strong>
              </td>
            </tr>
          </tbody>
        </table>
      </section>
      <section id="customer-details">
        <h1 style={{marginBottom:"1em"}}>Customer Information</h1>
        <ul >
            <div style={{textAlign:"left"}}>Name: {orderDetails.customer.name}</div>
            <div style={{textAlign:"left"}}>Address: {orderDetails.customer.address}</div>
            <div style={{textAlign:"left"}}>Email: {orderDetails.customer.email}</div>
            <div style={{textAlign:"left"}}>Card number: ****{orderDetails.customer.ccNumber.substring(orderDetails.customer.ccNumber.length-4)}</div>
            <div style={{textAlign:"left"}}>Expiration Date: {customerDate.getFullYear()}/{customerDate.getMonth()+1}</div>
        </ul>
      </section>
    </div>
  );
}
