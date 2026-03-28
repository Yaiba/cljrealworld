(ns frontend.views.login)

(defn render-ui [email password]
  [:div#login
   [:h1 "Sign in"]
   [:span "Email: "
    [:input {:type "text"
             :value email
             :placeholder "email"
             :on {:input [:app/set-email [:event.target/value]]}}]]
   [:span "Password: "
    [:input {:type "password"
             :value password
             :on {:input [:app/set-password [:event.target/value]]}}]]
   [:button {:on {:click [:app/login]}} "Sign in"]])
